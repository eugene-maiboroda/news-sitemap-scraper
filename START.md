# NYPost Article Pipeline — можливості та запуск оновлення

Це контекст і інструкція для колеги та AI-асистента. Документ стосується **гілки `feat/nypost-refactoring`** репозиторію `si-article-pipeline-refactored`, сайту NYPost і чинної реалізації в цій гілці. Не переносити сюди профілі, MENU Discovery, RAW_HTML або властивості NRA-ILA/Hunter з інших гілок. Перед змінами читати AGENTS.md і docs/progress.md. Commit/push виконує користувач; БД, Discovery та скачування запускає оператор, не AI без окремого доручення.

## Що вміє застосунок

Java 21+, Maven, Spring Boot 4.0.1, PostgreSQL. Точка входу — `com.article.Main`. Одноразовий console pipeline: Spring створює context, виконує вибрані кроки й завершує його. Це не постійний сервер або автоматичний scheduler.

Порядок: Discovery → парсинг → скачування фото → скачування відео → обробка авторів → експорт авторів → JSON статей. Кожен крок вмикається незалежно. Для контрольованого оновлення запускати по одному кроку, перевіряючи результат перед наступним.

- **Discovery:** читає річні sitemap NYPost, потім кожен денний sitemap; додає відсутні URL у article_links зі статусом NEW і ручним collection_batch_id. Річний index містить sitemap/loc, денний — url/loc і lastmod. Підставити денний urlset замість річного index зараз недостатньо: service очікує два рівні.
- **Парсинг:** обробляє тільки NEW; зберігає articles, category, topics, tags, mainCategory та метадані медіа. Успіх → PARSED, повернена парсером помилка → ERROR. Не кожен виняток гарантовано стає ERROR: перевіряти лог. Дата article_date/publishedDate у цьому потоці береться із sitemap lastmod, тому це не гарантована дата першої публікації.
- **Медіа:** окремі кроки читають PENDING із image/videos. Успіх → DOWNLOADED із local_file_path/file_size, невдача → FAILED. Файли не скачуються лише від увімкнення парсингу.
- **Автори:** process-authors знаходить кандидатів у статтях, додає невідомих авторів, перечитує профілі авторів і завантажує відсутні аватари. Export-authors окремо пише AllAuthors.json.
- **JSON:** читає збережені статті й метадані медіа з БД; не перепарсує сайт. Підтримує весь набір або один collection batch та різні способи групування.

## Значення collection batch

```properties
collector.collection-batch-id=update-001
export.collection-batch-id=
```

Collector ID позначає логічне оновлення. Для продовження завтра/після перезапуску залишити update-001; для наступної поставки вручну змінити на update-002. Чинний INSERT явно записує значення тільки новим URL. Порожній collector ID відхиляється до непорожньої вставки; export-only не вимагає його. Дати/номери не генеруються автоматично.

ON CONFLICT(article_url) DO NOTHING зберігає ID, статус і batch уже відомого URL. NEW → PARSED/ERROR теж не змінює batch. Це не exported flag: один і той самий batch можна експортувати повторно або в різних mode. ID article_links і articles не треба вважати однаковими: експорт поєднує їх через article_url.

- `export.collection-batch-id=` або відсутній ключ: усі articles, без batch-фільтра.
- `export.collection-batch-id=update-001`: тільки articles, URL яких належать цьому batch у article_links.
- `export.collection-batch-id=initial`: початковий набір, позначений міграцією.
- Невідомий batch: порожня вибірка, без fallback на все. Blank/пробіли обробляються як відсутність фільтра; краї непорожнього ID обрізаються.

Export batch впливає **тільки на вибірку JSON статей**. Парсинг читає всі NEW, скачування — всі відповідні PENDING, автори — свій глобальний набір. Не заявляти, що вони обмежені update-001.

## Topics, tags, category та export.mode

Topics — зібрані з верхніх тематичних позначок сторінки назви/URL, які використовуються для побудови ієрархії. Tags — назви/URL тегів із футера статті. Category — категорія з верхньої частини статті; mainCategory додатково визначається resolver-ом за першим topic. Це збережені дані статті, не меню Discovery.

Topics/tags збираються разом під час звичайного парсингу. Окремо парсити статті тричі не потрібно. topics_extracted=true встановлюється mapper-ом після такого збереження; це не ознака експорту й не гарантія непорожніх списків. Окремого extraction/backfill-кроку для topics_extracted=false тут немає.

| export.mode | Результат |
| --- | --- |
| category | JSON-файли за category статті. Default за відсутності mode — CATEGORY. |
| topics | JSON-файли за URL topics та побудованими ієрархіями. Одна стаття може потрапити в кілька файлів. |
| tags | Спеціальна вибірка Sports Betting: потрібні Sports Betting та відповідний NFL/NBA/MLB/NHL Betting tag. Формує nfl_betting.json, nba_betting.json, mlb_betting.json, nhl_betting.json. Це **не експорт усіх тегів**. |

Усі mode використовують той самий DTO статті: id, category, articleUrl, title, subTitle, publishedDate, author/authorLink, images, videos, topics, tags, content. Групування змінюється resolver-ом. Category-файл має об’єкт articles, topic/tag-файл — об’єкт URL з hierarchy та articles. Поточне `export.mode=tags` не підходить для передачі всіх звичайних новин, навіть якщо batch вибраний правильно.

Потрібні три представлення — парсинг один раз, потім три окремі експортні запуски з різними mode та output-директоріями. Передавати лише ті представлення, які замовив клієнт.

## Поточна конфігурація гілки

Джерело істини — src/main/resources/application.properties. Значення нижче звірені з файлом під час підготовки документа; перед запуском перечитати файл, бо оператор може змінити його. Пароль сюди не копіюємо.

| Ключ | Поточне значення / значення для запуску |
| --- | --- |
| spring.datasource.url | PostgreSQL nypost_articles на localhost:5437; повний JDBC URL у локальному properties |
| spring.datasource.username/password | Налаштовані локально; звірити з PostgreSQL, не переносити секрети в документацію |
| spring.main.web-application-type | none |
| spring.jpa.hibernate.ddl-auto | validate — перевіряє схему, не створює міграцій |
| spring.jpa.show-sql | false |
| logging.level.root / logging.level.com.article / logging.level.com.article.adapter.video | INFO / INFO / INFO |
| steps.discover-links.enabled / batch-size | false / 5000 |
| steps.parse-pages.enabled / batch-size | false / 5000 |
| steps.download-images.enabled / batch-size | false / 5000 |
| steps.download-videos.enabled / batch-size | false / 10 |
| steps.process-authors.enabled / batch-size | false / 2000 |
| steps.export-authors.enabled | false |
| steps.export-json.enabled / batch-size | false / 5000; batch-size додано під час фінальної перевірки, інакше enabled export отримував би 0 |
| steps.extract-connatix-videos.enabled | false |
| browser.enabled / headless / timeout-ms | false / true / 30000 |
| video.timeout-ms / yt-dlp-command | 300 / yt-dlp; попри назву timeout-ms, поточний VideoDownloadAdapter використовує це число як **секунди** |
| collector.parse-threads | 15 — спільна паралельна обробка статей/медіа/авторів |
| collector.sitemap-delay-ms | 150 — пауза перед денним sitemap |
| collector.collection-batch-id | update-001 |
| collector.sitemap-urls[0] | https://nypost.com/sitemap-2025.xml |
| collector.sitemap-urls[1] | https://nypost.com/sitemap-2026.xml |
| export.mode | tags |
| export.collection-batch-id | порожньо — повний набір |
| storage.export-dir | /Users/eugene/workspace/dataox/eyalProject2/exportJson |
| storage.images-dir | /Users/eugene/workspace/dataox/eyalProject2/articles-foto |
| storage.videos-dir | /Users/eugene/workspace/dataox/eyalProject2/articles-video |
| storage.avatars-dir | /Users/eugene/workspace/dataox/eyalProject2/author-avatars |

У цій гілці немає потреби задавати collector.site, collector.discovery-source чи профіль nrlila. Усі batch-size увімкнених Step мають бути >0; export-authors і extract-connatix — ToggleStep без batch-size. Batch-size обмежує розмір пакета, а не загальну кількість статей: цикл продовжується до завершення черги.

## Обов’язково: власні директорії на комп’ютері колеги

До будь-якого запуску змінити **чотири** ключі `storage.*` у `src/main/resources/application.properties`. Наявні абсолютні шляхи `/Users/eugene/...` належать комп’ютеру попереднього виконавця й не повинні залишатися для роботи колеги. Вказати власні доступні для запису директорії; приклад нижче використовує `/absolute/path` як позначення, яке потрібно замінити своїм реальним шляхом:

```properties
storage.export-dir=/absolute/path/nypost/export
storage.images-dir=/absolute/path/nypost/images
storage.videos-dir=/absolute/path/nypost/videos
storage.avatars-dir=/absolute/path/nypost/avatars
```

`export-dir` — JSON статей і авторів; `images-dir` — скачані фото; `videos-dir` — скачані відео; `avatars-dir` — аватари авторів. Для кожної поставки/режиму експорту обирати окрему output-директорію. Переконатися, що на диску достатньо місця.

Дамп БД не містить самих медіафайлів. Зміна цих властивостей не переносить старі файли й не переписує `local_file_path` або статуси `DOWNLOADED` у відновленій БД. Якщо потрібні вже скачані матеріали, передати їх окремо й узгодити розташування; не очікувати, що вони автоматично скачаються повторно після зміни директорії.

## Перед першим запуском із batch

1. Спочатку налаштувати чотири власні директорії `storage.*` за розділом вище. Перевірити git branch --show-current: feat/nypost-refactoring. Не перемикати брудне робоче дерево без окремого рішення.
2. Оператор запускає свій PostgreSQL NYPost. Compose цієї гілки: docker/docker-compose.yml, сервіс si-postgres, postgres:15, БД nypost_articles, host port 5437. Не запускати Hunter/Rifleman замість нього й не видаляти том із накопиченими даними.
3. Перевірити JDBC URL/облікові дані та наявність існуючих таблиць. Це не інструкція перебудови БД. Нове розгортання потребує чинної повної схеми окремо; ddl-auto=validate її не створить.
4. Задати collector.collection-batch-id для поточного збору. Нові URL зберігаються з цією назвою batch; потім її можна вказати в export.collection-batch-id, щоб експортувати саме цей набір.

5. Перевірити потрібні локальні директорії й залежності. Для JSON без скачування yt-dlp/ffmpeg і браузер не потрібні; для video download потрібен yt-dlp і залежності його конкретних джерел. Для Connatix потрібні Playwright Chromium та обидва прапорці нижче.
6. Узгодити з клієнтом роки/джерела, повний JSON чи дельту, потрібний mode, медіафайли та авторів. Точну межу попереднього збору не виводити лише з MAX(article_date).

Команда оператора після перевірки налаштувань:

```sh
mvn spring-boot:run
```

В IntelliJ запустити com.article.Main. Це реальний запуск вибраних кроків із підключенням до БД, не перевірка конфігурації. Для compile без збору: `mvn -o -q -DskipTests test-compile`.

## Покрокове оновлення існуючих даних

Перед кожним запуском вимикати всі кроки, потім вмикати тільки потрібний. В application.properties редагувати існуючі ключі, не створювати дублікати з різними значеннями:

```properties
steps.discover-links.enabled=false
steps.parse-pages.enabled=false
steps.download-images.enabled=false
steps.download-videos.enabled=false
steps.process-authors.enabled=false
steps.export-authors.enabled=false
steps.export-json.enabled=false
steps.extract-connatix-videos.enabled=false
browser.enabled=false
```

### 1. Discovery нових URL

Залишивши решту кроків вимкненими:

```properties
collector.collection-batch-id=update-001
collector.sitemap-urls[0]=https://nypost.com/sitemap-2025.xml
collector.sitemap-urls[1]=https://nypost.com/sitemap-2026.xml
steps.discover-links.enabled=true
```

Роки — поточний приклад, вибрати потрібні за попередньою поставкою. Перечитати роки можна: старі PARSED не стають NEW. Ідентичність — точний URL, аліаси автоматично не зливаються. Перевірити логи всіх денних sitemap: service логує помилки й продовжує, а підсумковий «finished» не доводить повноту. Невдалі дні повторити тим самим batch. Після перевірки встановити discover-links.enabled=false.

### 2. Парсинг

```properties
steps.discover-links.enabled=false
steps.parse-pages.enabled=true
```

Скачування/автори/експорт лишаються false. Збираються тексти, taxonomy та медіаметадані. Обробляється вся NEW-черга: спочатку перевірити старі NEW. Старі PARSED не перечитуються. Після завершення перевірити помилки та встановити parse-pages.enabled=false. Для повтору ERROR не скидати всі записи — спочатку визначити конкретні невдалі URL; автоматичного retry ERROR цим кроком немає. Batch зберігається при зміні статусу.

### 3. Фото й відео — тільки якщо потрібні файли

Окремо ввімкнути потрібний крок; решту залишити false:

```properties
steps.download-images.enabled=true
# Або окремим запуском:
# steps.download-videos.enabled=true
```

Після кожного запуску вимкнути його. Фото: HTTP, файл через .part і переміщення; наявний цільовий файл пропускається без перевірки ненульового розміру. Відео: yt-dlp для JW_TOP/YouTube, цільовий непорожній .mp4 перевіряється; Connatix не скачується цим кроком. DOWNLOADED не потрапляє в чергу, FAILED не повторюється автоматично. Якщо DOWNLOADED файл видалили з диска, сам повтор кроку його не відновить. Якщо старі PENDING залишилися, вони також обробляться незалежно від batch експорту.

### 4. Автори — окрема можливість

За потреби клієнта встановити steps.process-authors.enabled=true; після перевірки false. Крок може перечитувати вже наявних авторів, не лише авторів нового batch. Для JSON усіх збережених авторів окремо ввімкнути steps.export-authors.enabled=true; результат AllAuthors.json. Цей експорт теж не фільтрується export.collection-batch-id.

### 5. JSON нової поставки

```properties
steps.export-json.enabled=true
steps.export-json.batch-size=5000
export.collection-batch-id=update-001
export.mode=topics
```

Усі інші enabled — false. `topics` тут сценарій групування; якщо клієнту потрібні category або спеціальний Sports Betting tags, задати відповідний mode. Перед запуском вибрати окремий storage.export-dir для цієї поставки й представлення (оператор задає власний шлях).

JsonExportFileWriter **пересоздає цільові файли**, а не дописує в минулу поставку: APPEND використовується тільки між пакетами одного запуска. Файли, яких новий запуск не торкнувся, автоматично не очищаються, тому нова output-директорія важлива. Не перезапускати в старий каталог, очікуючи автоматичного злиття дельт. При нульовій вибірці файли можуть не створитися, а старі файли каталогу залишитися.

Після завершення перевірити JSON, кількості/URL/ієрархії й потрібні медіафайли; експорт метаданих медіа не доводить їхнього скачування. Переданий JSON є файлом експорту, не підтвердженням успішного імпорту клієнтом. Встановити export-json.enabled=false. Для повтору тієї самої поставки залишити export batch незмінним.

### 6. Повний JSON: старі + нові

Повторне скачування статей не потрібне. Вимкнути збір/парсинг/медіа/авторів, поставити:

```properties
steps.export-json.enabled=true
steps.export-json.batch-size=5000
export.collection-batch-id=
export.mode=topics
```

Порожній batch означає всі збережені articles; mode все одно визначає групування/фільтрацію resolver-ом (tags не видасть усі статті). Batch/статуси в БД не скидати. Для наступного оновлення почати з нового collector ID, наприклад update-002. Якщо клієнт просить саме перепарсити зміст старих статей, це інша операція, тут вона не реалізується скиданням усіх PARSED.

## Початковий збір та інші можливості

Для першого збору у вже підготовлену порожню БД той самий порядок, але можна вручну задати collector.collection-batch-id=initial і потрібні річні sitemap. Не запускати bootstrap SQL поверх робочої БД. На існуючій БД initial уже позначає старі записи, а нові оновлення повинні мати власні ID.

Connatix — додаткове збирання метаданих нижнього плеєра **під час парсингу NEW**, а не самостійне дозаповнення всієї БД:

```properties
steps.parse-pages.enabled=true
browser.enabled=true
steps.extract-connatix-videos.enabled=true
browser.headless=true
browser.timeout-ms=30000
```

Це дорожче: запускається браузер для перехоплення відповіді плеєра. Для звичайного оновлення без Connatix залишити browser.enabled=false і extract-connatix-videos=false. Для старих PARSED цей прапорець сам нічого не обробить. JW/YouTube extractors є частиною стандартного парсингу; скачування файлів — окремий крок.

## Read-only SQL для оператора

AI у поточній задачі БД не запускав/не читав. Ці запити оператор виконує сам у потрібній БД; вони не є результатами проведеної живої перевірки:

```sql
SELECT current_database();

SELECT collection_batch_id, status, COUNT(*)
FROM article_links
GROUP BY collection_batch_id, status
ORDER BY collection_batch_id, status;

SELECT COUNT(*) AS stored_articles_in_batch
FROM articles a
JOIN article_links l ON l.article_url = a.article_url
WHERE l.collection_batch_id = 'update-001';

SELECT COUNT(*) AS parsed_without_article
FROM article_links l
LEFT JOIN articles a ON a.article_url = l.article_url
WHERE l.status = 'PARSED' AND a.id IS NULL;

SELECT download_status, COUNT(*) FROM image GROUP BY download_status;
SELECT platform, download_status, COUNT(*) FROM videos GROUP BY platform, download_status;
```

## Архітектура, перевірки та передача AI

application оркеструє через порти; adapter/sitemap працює з XML, adapter/article — HTML, persistence — SQL/JPA, export — JSON, bootstrap/properties — налаштування. ArticleExportPort уже є: не додавати другий writer/порт заради full/batch. Поточний batch-фільтр — ArticleRepository native JOIN та routing у ArticlePersistenceService; порти/DTO JSON не розширені.

Перед зміною поведінки читати AGENTS, поточні props і docs/progress; не переносити назви класів/кроків із інших гілок. Нових тестів без запиту не додавати. Наявні тести не замінюють живу перевірку PostgreSQL: batch JOIN/INSERT і реальний експорт оператор перевіряє під час запуску. AppTest.contextLoads у цій гілці порожній і не є перевіркою запуску Spring.

Локальні HTML/headers із минулих аудитів лежать у work/, docs містить внутрішні design/plan/progress — обидва каталоги ігноруються Git. Їх не передавати як production-код, але можна використати як докази перевірок. Тимчасові Java harness у /tmp не входять до репозиторію. START.md є інструкцією передачі, без паролів.
