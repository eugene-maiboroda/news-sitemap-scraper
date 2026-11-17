# Article Pipeline — запуск і підключення сайту

Pipeline знаходить URL статей, парсить сторінки, зберігає дані в PostgreSQL і формує JSON для імпортера. Реалізований сайт — American Hunter. Java 21, Maven, Spring Boot, PostgreSQL 15 у наявному Docker Compose.

## Перший локальний запуск

1. Підніми PostgreSQL або запусти `docker compose -f docker/docker-compose.yml up -d`. Compose налаштований на БД `hunter_articles` і порт хоста `5441`. Для іншого PostgreSQL створи порожню БД під своїм користувачем.
2. З кореня репозиторію застосуй [схему Hunter](database/hunter-schema.sql) один раз до порожньої БД. Для наявної БД із даними потрібна окрема міграція.

   ```sh
   psql -h localhost -p 5441 -U YOUR_DB_USER -d hunter_articles \
     -v ON_ERROR_STOP=1 -f database/hunter-schema.sql
   ```

3. Передай `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` і `SPRING_DATASOURCE_PASSWORD` через локальне середовище або конфігурацію IDE. URL для Compose має вигляд `jdbc:postgresql://localhost:5441/hunter_articles`. Hibernate запускається з `ddl-auto=validate` і перевіряє вже створені таблиці.
4. Вкажи свої директорії та явно вибери кроки запуску. Поточний [application.properties](src/main/resources/application.properties) містить локальні шляхи й перемикачі робочої сесії.

   ```sh
   export STORAGE_EXPORT_DIR="$PWD/work/export"
   export STORAGE_IMAGES_DIR="$PWD/work/images"
   export STORAGE_VIDEOS_DIR="$PWD/work/videos"

   export STEPS_DISCOVERLINKS_ENABLED=false
   export STEPS_PARSEPAGES_ENABLED=false
   export STEPS_EXTRACTAUTHORS_ENABLED=false
   export STEPS_DOWNLOADIMAGES_ENABLED=false
   export STEPS_DOWNLOADVIDEOS_ENABLED=false
   export STEPS_EXPORTJSON_ENABLED=true

   mvn spring-boot:run
   ```

   Приклад дістає JSON з уже наповненої БД. Для порожньої БД спочатку запусти Discovery і HTTP-парсинг за налаштуваннями нижче. З IDE запускай `com.article.Main`. Для перевірки компіляції з кешованими залежностями використовуй `mvn -o -q -DskipTests test-compile`.

## Кроки і режими

`*.enabled=true` вмикає показаний крок, `false` його пропускає. Порядок звичайного запуску наведено в таблиці. Для окремого запуску одного кроку вимкни решту.

| Перемикач | Що відбувається |
| --- | --- |
| `steps.discover-links.enabled` | Заповнює чергу `article_links`. MENU збирає URL, заголовок, короткий опис картки й усі шляхи меню. SITEMAP збирає URL зі стандартного XML sitemap без дати та шляхів меню. |
| `steps.parse-pages.enabled` | У режимі HTTP завантажує сторінки з черги `NEW`. Зберігає метадані, тіло HTML, архівний HTML, партнерський текст, верхні й нижні мітки, Related Stories та посилання на медіа. Після парсингу Hunter окремо формує довідник штатів. |
| `steps.extract-authors.enabled` | Читає унікальні посилання на авторів зі збережених статей, завантажує профілі та пише `AllAuthors.json`. Селектори імені, біографії й фото визначені в `AuthorParser`. |
| `steps.download-images.enabled` | Завантажує фото з черги `PENDING`, записує шлях, розмір і результат завантаження в БД. |
| `steps.download-videos.enabled` | Завантажує відео з черги `PENDING` через `yt-dlp`, записує шлях, розмір і результат у БД. |
| `steps.export-json.enabled` | Пише `american-hunter-articles.json` зі збережених даних. Один запис на URL, усі розділи в `menuPaths`, медіа як URL із позиціями. Працює самостійно з вимкненим збором. |

`steps.parse-pages.source=HTTP` використовує живі сторінки. `RAW_HTML` у поточній реалізації Hunter читає збережений `articles.raw_html` і дозаповнює порожні `image.caption`. Коли парсинг увімкнено в режимі `RAW_HTML`, pipeline виконує лише це дозаповнення й завершується. Для експорту вимкни `steps.parse-pages.enabled`.

Довідник `state-fish-game-information.json` генерує окремий адаптер після HTTP-парсингу Hunter. Готовий локальний екземпляр, якщо присутній у робочому дереві, лежить у `work/hunter-parsing/`. Нові JSON пишуться в `storage.export-dir`.

## Конфігурація

| Ключ | Значення |
| --- | --- |
| `collector.site` | `AMERICAN_HUNTER`. Тип визначено в `CollectorProps.Site`, Spring вибирає адаптери за значенням властивості. |
| `collector.discovery-source` | `MENU` або `SITEMAP`. Для MENU використовується карта з адаптера сайту. |
| `collector.sitemap-urls[0]`, `[1]`, … | Стартові URL для SITEMAP. Можна вказати індекс або окремий URL set. Для меню ці налаштування не потрібні. |
| `collector.parse-threads` | Кількість потоків спільного виконавця, який використовують парсинг, збір авторів і завантаження медіа. |
| `steps.parse-pages.batch-size` | Розмір пакета парсингу. У RAW_HTML застосовується окреме обмеження в `ParseArticleUseCase`. |
| `steps.download-images.batch-size`, `steps.download-videos.batch-size` | Розмір пакетів черг завантаження. |
| `steps.export-json.batch-size` | Додатний розмір пакета читання для експорту. |
| `steps.discover-links.batch-size` | Є в конфігурації, поточний Discovery не читає це значення. JDBC-запис має власні розміри пакетів. |
| `storage.export-dir`, `storage.images-dir`, `storage.videos-dir` | Директорії JSON, завантажених фото й відео. |
| `browser.enabled`, `browser.headless`, `browser.timeout-ms` | Опційний Playwright і його параметри. MENU, SITEMAP та HTTP-парсер Hunter працюють без браузера. |
| `video.yt-dlp-command`, `video.timeout-ms` | Команда/шлях до yt-dlp і таймаут. Поточний адаптер трактує числове значення таймауту в секундах, попри назву ключа. |

## Підключення нового сайту

1. **Почни з меню.** Вручну склади таблицю «URL розділу → усі ієрархії від кореня». Познач розділи зі списком статей і довідкові сторінки. Додай зразки HTML карток і пагінації. Наприклад, один шлях до Archery може бути `["Archery"]`, інший — `["Guns & Gear", "Archery"]`, хоча URL розділу спільний.
2. **Реалізуй Discovery.** Візьми за орієнтир [AmericanHunterDiscoveryAdapter](src/main/java/com/article/adapter/discovery/AmericanHunterDiscoveryAdapter.java) в `adapter/discovery`. Новий адаптер реалізує [MenuDiscoveryPort](src/main/java/com/article/port/MenuDiscoveryPort.java). `sections()` повертає `Section(path, hierarchies, collectCards)`, а `discover(Section)` — `SectionResult(cards, error)`. Збирай картки `Card(articleUrl, title, description)` з абсолютними нормалізованими URL, перевір пагінацію й повтори. `collectCards=false` виключає довідкову сторінку зі збору карток. Application сам додає `menu_paths` і передає результат у спільний upsert.
3. **Зареєструй сайт.** Додай значення в `CollectorProps.Site` і підключи адаптер через `@ConditionalOnProperty(name = "collector.site", havingValue = "NEW_SITE")`, підставивши фактичне значення. Додай відповідну умову і до наявного `AmericanHunterDiscoveryAdapter`, який зараз реєструється безумовно, щоб Spring бачив один `MenuDiscoveryPort` для вибраного сайту. Перевір усі обов’язкові Spring-залежності для вибраного сайту. `DiscoveryLinkUseCase` отримує `MenuDiscoveryPort` навіть у режимі SITEMAP, а парсинг має залежності від `ArticlePagePort` і `ParsedArticleStorePort` навіть із вимкненим кроком. Для першого запуску нового сайту потрібні сумісні реалізації або окремо погоджена зміна підключення цих компонентів.
4. **Перевір Discovery окремо.** Увімкни тільки `steps.discover-links.enabled`, перевір кілька розділів на окремій локальній БД. Один URL у кількох розділах має створити один рядок із повною картою `menu_paths`. Якщо потрібен SITEMAP, використай спільний `SitemapPort` і врахуй, що сам XML не дає шляхів меню.
5. **Додай парсинг і збереження.** Передай приклади статей із різною розміткою. Реалізуй `ArticlePagePort` і підключи сумісний `ParsedArticleStorePort` для нового сайту. Визнач джерела заголовка, дати, автора, тіла, міток і медіа. Перевір повторний запис і помилки. Окремо визнач, чи потрібен цьому сайту режим повторного читання HTML.
6. **Погодь клієнтський JSON.** Реалізуй спільний `ArticleJsonExportPort` у пакеті нового сайту. JDBC-читання й DTO розмісти поруч з адаптером, використай спільний `JsonExportAdapter`. Запусти тільки експорт і звір кількість записів, унікальні URL, шляхи й медіа з БД.

Для передачі задачі в ChatGPT дай цей гайд разом із кодом згаданих портів і чинних адаптерів, картою меню та HTML-зразками. Початковий результат — перевірена карта `Section` і Discovery для одного розділу. Розширюй збір після перевірки кількох розділів і повторного запуску.
