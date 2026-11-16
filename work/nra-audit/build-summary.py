from pathlib import Path
import json,hashlib,re,datetime
base=Path('work/nra-audit')
sources={
'rifleman-taurus.html':'https://www.americanrifleman.org/content/rifleman-review-taurus-raging-hunter-in-350-legend/',
'rifleman-whitworth.html':'https://www.americanrifleman.org/content/i-have-this-old-gun-whitworth-rifle/',
'rifleman-saltstone.html':'https://www.americanrifleman.org/content/saltstone-integrator-rifle-carry-system-modular-hunting-vest-the-cure-for-the-common-sling/',
'rifleman-old.html':'https://www.americanrifleman.org/content/webley-mk4-revolver/',
'rifleman-reviews-guns.html':'https://www.americanrifleman.org/reviews/guns/',
'rifleman-reviews-gear.html':'https://www.americanrifleman.org/reviews/gear/',
'rifleman-reviews.html':'https://www.americanrifleman.org/reviews/',
'rifleman-news-guns.html':'https://www.americanrifleman.org/news/guns/',
'rifleman-2026.html':'https://www.americanrifleman.org/news/new-for-2026/',
'rifleman-gear-js.html':'https://www.americanrifleman.org/assets/js/all.min.js?v=25',
'rifleman-guns-p50.html':'https://www.americanrifleman.org/reviews/guns/?pg=50&it=10&st=True&sd=Descending&cnt=All&cat=1218&f=latest',
'rifleman-guns-p51.html':'https://www.americanrifleman.org/reviews/guns/?pg=51&it=10&st=True&sd=Descending&cnt=All&cat=1218&f=latest',
'rifleman-guns-p50-default.html':'https://www.americanrifleman.org/reviews/guns/?pg=50',
'rifleman-guns-it100.html':'https://www.americanrifleman.org/reviews/guns/?pg=1&it=100',
'rifleman-guns-it100-p34.html':'https://www.americanrifleman.org/reviews/guns/?pg=34&it=100',
'hunter-home.html':'https://www.americanhunter.org/',
'hunter-video.html':'https://www.americanhunter.org/videos/',
'hunter-benelli.html':'https://www.americanhunter.org/content/sundaygunday-benelli-ethos-cordoba/',
'hunter-review.html':'https://www.americanhunter.org/content/bullshooters-dryfire-review/',
'hunter-oldest.html':'https://www.americanhunter.org/content/hunting-small-game-an-overview/',
'hunter-rifles.html':'https://www.americanhunter.org/guns-gear/rifles/',
'hunter-rifles-it100.html':'https://www.americanhunter.org/guns-gear/rifles/?pg=1&it=100',
'ila-article.html':'https://www.nraila.org/articles/20261022/new-york-gov-hochul-signs-more-gun-control-this-week',
'ila-map-local.html':'https://www.nraila.org/sitemap-local/',
'ila-map-shared.html':'https://www.nraila.org/sitemap-shared-content/',
'ila-news.html':'https://www.nraila.org/news/articles/',
'ila-videos.html':'https://www.nraila.org/videos/',
'ila-2009.html':'https://www.nraila.org/news/articles/?page=1&startDate=20090101&endDate=20091231&contentBuckets=8185',
'ila-2020.html':'https://www.nraila.org/news/articles/?page=1&startDate=20200101&endDate=20201231&contentBuckets=8185',
'ila-old-article.html':'https://www.nraila.org/articles/20091205/camouflagethe-original-green-2',
'ila-video-legacy.html':'https://www.nraila.org/media/20170327/video/what-is-national-concealed-carry-reciprocity',
'ila-video-update.html':'https://www.nraila.org/articles/20250421/legal-update-april-2025-litigation-update',
'nra-home.html':'https://www.nra.org/',
'nra-news.txt':'https://www.nra.org/news/',
'nra-taurus.html':'https://www.nra.org/news/rifleman-review-taurus-raging-hunter-in-350-legend',
'nra-videos.html':'https://www.nra.org/videos',
'nra-video-series.html':'https://www.nra.org/videos/rifleman-review',
'ila-video-mp4-headers.txt':'https://d3ldmcwnurseh3.cloudfront.net/assets/what-is-national-concealed-carry-reciprocity-/what-is-national-concealed-carry-reciprocity-.mp4',
}
for n,host,mp in [('rifleman','www.americanrifleman.org','/sitemap/'),('hunter','www.americanhunter.org','/sitemap/'),('ila','www.nraila.org','/Sitemap'),('nra','www.nra.org','/sitemap.xml')]:
 sources[n+'-robots.txt']='https://'+host+'/robots.txt'
 sources[n+'-sitemap.txt']='https://'+host+'/sitemap.xml'
 if n!='nra': sources[n+'-map.txt']='https://'+host+mp
manifest=[]
for name,url in sources.items():
 p=base/name
 if not p.exists():raise RuntimeError(name)
 manifest.append({'file':name,'url':url,'method':'HEAD' if name.endswith('-headers.txt') else 'GET','captured_at_utc':datetime.datetime.fromtimestamp(p.stat().st_mtime,datetime.timezone.utc).isoformat(),'bytes':p.stat().st_size,'sha256':hashlib.sha256(p.read_bytes()).hexdigest()})
(base/'sources.json').write_text(json.dumps(manifest,indent=2,ensure_ascii=False))
evidence={r['file']:r for r in json.loads((base/'evidence.json').read_text())}
metrics={'verified_on':'2026-10-23','scope':'Sample-based technical audit; not a full crawl','sitemaps':{}}
for n in ['rifleman','hunter','nra']:
 a=json.loads((base/(n+'-urls.json')).read_text());prefix='/content/' if n!='nra' else '/news/'
 c=[x for x in a if prefix in x['url']]
 metrics['sitemaps'][n]={'records':len(a),'unique_urls':len({x['url'] for x in a}),'content_records':len(c),'unique_content_urls':len({x['url'] for x in c}),'missing_lastmod':sum(x['lastmod'] is None for x in a)}
for n in ['ila-map-local','ila-map-shared']:
 a=json.loads((base/(n+'-urls.json')).read_text());metrics['sitemaps'][n]={'records':len(a),'article_records':sum(bool(re.search(r'https://www\.nraila\.org/articles/\d{8}/', x)) for x in a)}
listing='.latest-stories-list h3 a'
metrics['latest_reviews_lists_identical']=evidence['rifleman-reviews.html'][listing]==evidence['rifleman-reviews-guns.html'][listing]==evidence['rifleman-reviews-gear.html'][listing]
metrics['pg51_repeats_pg50']=evidence['rifleman-guns-p50.html'][listing]==evidence['rifleman-guns-p51.html'][listing]
metrics['pagination']={n:{'label':evidence[n+'.html']['.paging-copy'],'items':len(evidence[n+'.html'][listing])} for n in ['rifleman-guns-it100','rifleman-guns-it100-p34','hunter-rifles-it100']}
(base/'metrics.json').write_text(json.dumps(metrics,indent=2,ensure_ascii=False))
print(json.dumps(metrics,indent=2,ensure_ascii=False))
