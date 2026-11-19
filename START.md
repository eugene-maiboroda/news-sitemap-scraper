# Article Pipeline — запуск і підключення сайту

Pipeline знаходить URL статей, парсить сторінки, зберігає дані в PostgreSQL і формує JSON для імпортера. Реалізований сайт — American Hunter. Java 21, Maven, Spring Boot, PostgreSQL 15 у наявному Docker Compose.

## Профілі Hunter і Rifleman

Спільні налаштування лежать в application.properties. application-hunter.properties вибирає AMERICAN_HUNTER та hunter_articles на порту 5441, application-rifleman.properties — AMERICAN_RIFLEMAN та rifleman_articles на порту 5442. Типовий профіль без явного вибору — hunter. Усі кроки за замовчуванням вимкнені.

Запусти відповідний PostgreSQL вручну в Docker Desktop. Логін і пароль підключення задані безпосередньо у відповідному application-hunter.properties або application-rifleman.properties. Профілі використовують чинного користувача eyal і попередні storage-директорії. В IntelliJ поле Active profiles встанови в hunter або rifleman, або передай програмний аргумент --spring.profiles.active=hunter.

Через Maven:

```sh
mvn spring-boot:run -Dspring-boot.run.profiles=hunter
mvn spring-boot:run -Dspring-boot.run.profiles=rifleman
```

Для експорту додай до обраного запуску аргумент --steps.export-json.enabled=true. У Maven його можна передати через -Dspring-boot.run.arguments="--steps.export-json.enabled=true". Параметри кроків задаються у спільному application.properties або аргументах конкретного запуску.

## Перший локальний запуск

1. Перевір `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` і порт у [docker-compose.yml](docker/docker-compose.yml). Для Hunter це сервіс `si-postgres-hunter`, БД `hunter_articles` і порт хоста `5441`. Запусти `docker compose -f docker/docker-compose.yml up -d si-postgres-hunter`. Якщо використовуєш свій PostgreSQL, створи порожню БД з вибраною назвою під своїм користувачем.
2. З кореня репозиторію застосуй [схему Hunter](database/hunter-schema.sql) один раз до порожньої БД. Для наявної БД із даними потрібна окрема міграція.

   ```sh
   psql -h localhost -p 5441 -U YOUR_DB_USER -d hunter_articles \
     -v ON_ERROR_STOP=1 -f database/hunter-schema.sql
   ```

3. Укажи в `application.properties` або в локальних параметрах запуску JDBC URL з тим самим портом хоста й назвою БД. Для Hunter це `spring.datasource.url=jdbc:postgresql://localhost:5441/hunter_articles`. `spring.datasource.username` і `spring.datasource.password` мають відповідати обліковим даним PostgreSQL. Hibernate перевіряє вже створені таблиці через `ddl-auto=validate`.
4. Вкажи свої директорії та явно вибери кроки запуску. У [application.properties](src/main/resources/application.properties) усі кроки вимкнені за замовчуванням, а директорії задані відносно кореня проєкту.

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

## Окрема БД для нового сайту

Перед першим запуском нового сайту узгодь значення в Compose, команді `psql` і конфігурації застосунку. Приклад нижче використовує умовну назву `newsite`.

| Що змінити | Hunter | Приклад для нового сайту |
| --- | --- | --- |
| Ключ сервісу під `services` | `si-postgres-hunter` | `si-postgres-newsite` |
| `environment.POSTGRES_DB` сервісу | `hunter_articles` | `newsite_articles` |
| Прив’язка порту хоста до PostgreSQL | `5441:5432` | `5442:5432` |
| Ім’я тому в сервісі та в кореневому `volumes` | `si_article_hunter_pgdata` | `si_article_newsite_pgdata` |
| `spring.datasource.url` | `jdbc:postgresql://localhost:5441/hunter_articles` | `jdbc:postgresql://localhost:5442/newsite_articles` |
| Аргументи `psql` | `-p 5441 -d hunter_articles` | `-p 5442 -d newsite_articles` |

У прив’язці порту змінюється ліва частина. Шлях тому всередині контейнера лишається `/var/lib/postgresql/data`. Якщо хочеш тримати обидві БД запущеними, додай новий сервіс і новий том поруч із Hunter та вкажи назву нового сервісу в команді `docker compose up -d`. PostgreSQL застосовує `POSTGRES_DB` під час першої ініціалізації порожнього тому. Зміна цього значення зі старим томом сама по собі нову БД не створює.

Логін і пароль можна залишити такими самими або змінити. Якщо змінюєш, узгодь `POSTGRES_USER` і `POSTGRES_PASSWORD` із налаштуваннями підключення Spring, а в `psql -U` передай відповідний логін. Після запуску нового сервісу застосуй SQL-схему до нової БД до старту застосунку.

### Створення БД і таблиць

Для Rifleman із новим порожнім томом офіційний образ PostgreSQL автоматично створить `rifleman_articles` зі значення `POSTGRES_DB`. Запуск і перевірка з хоста:

```sh
docker compose -f docker/docker-compose.yml up -d si-postgres-rifleman
psql -h localhost -p 5442 -U eyal -d postgres -c '\l'
```

Дочекайся готовності PostgreSQL і перевір, що `rifleman_articles` є в списку. Якщо сервер доступний, але цієї БД немає, наприклад через повторно використаний том, створи її вручну. Підключайся до службової БД `postgres` під користувачем із правом створювати БД:

```sh
psql -h localhost -p 5442 -U eyal -d postgres \
  -v ON_ERROR_STOP=1 -c 'CREATE DATABASE rifleman_articles;'
```

Коли цільова БД існує, створи в ній таблиці. Якщо новий сайт використовує ту саму схему, застосуй наявний скрипт:

```sh
psql -h localhost -p 5442 -U eyal -d rifleman_articles \
  -v ON_ERROR_STOP=1 -f database/hunter-schema.sql
```

`hunter-schema.sql` створює таблиці та індекси в БД, яку передано через `-d`. Для Hunter підстав `5441` і `hunter_articles`.

## Кроки і режими

`*.enabled=true` вмикає показаний крок, `false` його пропускає. Порядок звичайного запуску наведено в таблиці. Для окремого запуску одного кроку вимкни решту.

| Перемикач | Що відбувається |
| --- | --- |
| `steps.discover-links.enabled` | Заповнює чергу `article_links`. MENU збирає URL, заголовок, короткий опис картки й усі шляхи меню. SITEMAP збирає URL зі стандартного XML sitemap без дати та шляхів меню. |
| `steps.parse-pages.enabled` | У режимі HTTP завантажує сторінки з черги `NEW`. Зберігає метадані, тіло HTML, архівний HTML, партнерський текст, верхні й нижні мітки, Related Stories та посилання на медіа. Після парсингу Hunter окремо формує довідник штатів. |
| `steps.extract-authors.enabled` | Читає унікальні посилання на авторів зі збережених статей, завантажує профілі та пише `AllAuthors.json`. Селектори імені, біографії й фото визначені в `AuthorParser`. |
| `steps.download-images.enabled` | Завантажує фото з черги `PENDING`, записує шлях, розмір і результат завантаження в БД. |
| `steps.download-videos.enabled` | Завантажує відео з черги `PENDING` через `yt-dlp`, записує шлях, розмір і результат у БД. |
| `steps.export-json.enabled` | Пише JSON статей зі збережених даних. Один запис на URL, усі розділи в `menuPaths`, медіа як URL із позиціями. Працює самостійно з вимкненим збором. |

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
| `steps.export-json.file-name` | Необов’язкова назва файлу в `storage.export-dir`. Типові назви — `american-hunter-articles.json` і `american-rifleman-articles.json` за `collector.site`. |
| `steps.export-json.batch-size` | Додатний розмір пакета читання для експорту. |
| `steps.discover-links.batch-size` | Є в конфігурації, поточний Discovery не читає це значення. JDBC-запис має власні розміри пакетів. |
| `storage.export-dir`, `storage.images-dir`, `storage.videos-dir` | Директорії JSON, завантажених фото й відео. |
| `browser.enabled`, `browser.headless`, `browser.timeout-ms` | Опційний Playwright і його параметри. MENU, SITEMAP та HTTP-парсер Hunter працюють без браузера. |
| `video.yt-dlp-command`, `video.timeout-ms` | Команда/шлях до yt-dlp і таймаут. Поточний адаптер трактує числове значення таймауту в секундах, попри назву ключа. |

## Передача новому чату й дослідження сайту

Перед початком прочитай кореневий AGENTS.md, локальний docs/progress.md, цей START.md, актуальні профілі та повний локальний скіл ../engineering-playbook/skills/brainstorming/SKILL.md. Для формату даних прочитай docs/specs/2026-11-17-american-hunter-export-design.md та уточнення docs/specs/2026-11-19-hunter-media-content-research.md. Старі звіти в work/local-audit — джерело гіпотез, їхні висновки перевіряй на поточному сайті.

Перший результат — дослідження нового сайту й погоджена карта меню. Користувач надає меню або зразки навігації. Звір URL, вкладеність і повторні входи до одного розділу, сформуй список Section(path, hierarchies, collectCards).

Перед реалізацією Discovery перевір:

- Що повертає звичайний HTTP GET: повний список, першу сторінку або каркас для JavaScript. Порівняй з видимою сторінкою.
- Чи можна отримати всі картки одним запитом через фактично підтримуваний параметр розміру сторінки. Перевір кількість, дублікати, останню картку та наявність Next. HTTP 200 сам по собі не доводить повноту.
- Якщо потрібна пагінація, визнач точні параметри page/pg, page size, View More або endpoint списку. Перевір першу, проміжну й останню сторінки, умову зупинки та повтор відповіді. Не припускай, що параметри Hunter підходять новому сайту.
- Чи є XML sitemap або публічний endpoint даних. Зістав їх із вимогою зберегти всі шляхи меню, опис картки й назву. Обери джерело на основі потрібних даних і фактичної відповіді.
- Відокрем Latest Stories від Featured, Most Popular, Related і реклами. Звір різні розділи, щоб спільна видача не дала помилкових menu_paths.
- Перевір доступність оригіналів фото, lazy-атрибути й типи відеоплеєрів на кількох статтях. Видалення параметрів URL узгоджуй із поведінкою конкретного хоста.

Збережи висновки, зразки URL, обмеження та рішення українською в design і progress. Після погодження дизайну підготуй план. Реалізуй спочатку Discovery одного розділу, потім кількох розділів із повторними URL, далі парсинг і експорт. Сумісність із загальною схемою БД та DTO перевіряй за даними нового сайту.

## Підключення нового сайту

1. **Почни з меню.** Вручну склади таблицю «URL розділу → усі ієрархії від кореня». Познач розділи зі списком статей і довідкові сторінки. Додай зразки HTML карток і пагінації. Наприклад, один шлях до Archery може бути `["Archery"]`, інший — `["Guns & Gear", "Archery"]`, хоча URL розділу спільний.
2. **Реалізуй Discovery.** Візьми за орієнтир [AmericanHunterDiscoveryAdapter](src/main/java/com/article/adapter/discovery/AmericanHunterDiscoveryAdapter.java) в `adapter/discovery`. Новий адаптер реалізує [MenuDiscoveryPort](src/main/java/com/article/port/MenuDiscoveryPort.java). `sections()` повертає `Section(path, hierarchies, collectCards)`, а `discover(Section)` — `SectionResult(cards, error)`. Збирай картки `Card(articleUrl, title, description)` з абсолютними нормалізованими URL, перевір пагінацію й повтори. `collectCards=false` виключає довідкову сторінку зі збору карток. Application сам додає `menu_paths` і передає результат у спільний upsert.
3. **Зареєструй сайт.** Додай значення в `CollectorProps.Site` і підключи адаптер через `@ConditionalOnProperty(name = "collector.site", havingValue = "NEW_SITE")`, підставивши фактичне значення. Додай відповідну умову і до наявного `AmericanHunterDiscoveryAdapter`, який зараз реєструється безумовно, щоб Spring бачив один `MenuDiscoveryPort` для вибраного сайту. Перевір усі обов’язкові Spring-залежності для вибраного сайту. `DiscoveryLinkUseCase` отримує `MenuDiscoveryPort` навіть у режимі SITEMAP, а парсинг має залежності від `ArticlePagePort` і `ParsedArticlePersistencePort` навіть із вимкненим кроком. Для першого запуску нового сайту потрібні сумісні реалізації або окремо погоджена зміна підключення цих компонентів.
4. **Перевір Discovery окремо.** Підготуй БД за розділом «Окрема БД для нового сайту». Увімкни тільки `steps.discover-links.enabled`, перевір кілька розділів на окремій локальній БД. Один URL у кількох розділах має створити один рядок із повною картою `menu_paths`. Якщо потрібен SITEMAP, використай спільний `SitemapPort` і врахуй, що сам XML не дає шляхів меню.
5. **Додай парсинг і збереження.** Передай приклади статей із різною розміткою. Реалізуй `ArticlePagePort` і підключи сумісний `ParsedArticlePersistencePort` для нового сайту. Визнач джерела заголовка, дати, автора, тіла, міток і медіа. Перевір повторний запис і помилки. Окремо визнач, чи потрібен цьому сайту режим повторного читання HTML.
6. **Погодь клієнтський JSON.** Для спільної схеми БД і JSON використай чинний `ArticleJsonExportAdapter` і за потреби задай `steps.export-json.file-name`. Якщо потрібен окремий формат, підключи відповідну реалізацію `ArticleJsonExportPort`. Запусти тільки експорт і звір кількість записів, унікальні URL, шляхи й медіа з БД.

Для передачі задачі в ChatGPT дай цей гайд разом із кодом згаданих портів і чинних адаптерів, картою меню та HTML-зразками. Початковий результат — перевірена карта `Section` і Discovery для одного розділу. Розширюй збір після перевірки кількох розділів і повторного запуску.

## Оновлення content і URL фото Hunter з архіву

Для вже зібраних статей Hunter вибери підключення до БД Hunter і такі параметри:

```properties
collector.site=AMERICAN_HUNTER
steps.parse-pages.enabled=true
steps.parse-pages.source=RAW_HTML
steps.parse-pages.archive-operation=MEDIA_CONTENT
steps.parse-pages.batch-size=100
steps.discover-links.enabled=false
steps.export-json.enabled=false
```

Операція читає збережений raw_html. Оновлює content з усього article-content без правої рекламної колонки та прибирає query/fragment з окремих URL фото Hunter у /media/. Фото всередині HTML зберігають початкові атрибути. ID, caption, метадані й інші поля БД зберігаються. Помилки зіставлення фото або відсутнього архіву логуються за URL, зміни такої статті відкочуються. Перевір підсумкові updated/failed.

Для чинного дозаповнення підписів використовуй archive-operation=CAPTIONS (типове значення). MEDIA_CONTENT реалізовано для Hunter і Rifleman. Режим RAW_HTML завершує pipeline після архівної операції. Після її перевірки запусти експорт окремо з parse-pages.enabled=false та export-json.enabled=true.

## Оновлення медіа American Rifleman

Профіль rifleman налаштований на RAW_HTML / MEDIA_CONTENT з batch-size=100 та вимкненими іншими кроками. Вибери його в Active profiles і запусти Main для оновлення content та окремих URL фото з БД rifleman_articles. Дія виконується за тим самим правилом, що для Hunter: весь article-content без правої колонки, головне фото із справжнім src, окремі власні /media/ URL без query і fragment, плеєри на початкових місцях.

Після перевірки updated/failed у application-rifleman.properties встанови steps.parse-pages.enabled=false і steps.export-json.enabled=true та запусти ще раз. Результат — american-rifleman-articles.json у storage.export-dir. Щоб повторно завантажувати сторінки з сайту, вибери HTTP і підготуй чергу NEW.
