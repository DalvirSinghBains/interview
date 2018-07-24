Overview
===========================
- Free tier of OneForge service supports 1000 free api calls. To support 10,000 api calls we need to use caching or else we might run out of quota.
- Instead of **convert** api we need to use the **quotes** api and fetch rates for all valid currency pairs in single api request
- In the implementation I have used `scalacache`(with configurable ttl) to cache the results of the api, 
	`http4s` for calling the api and `circe` to parse the json response
- There are two branches `interview` and `interview-squashed`. `interview` branch contains evolution of the feature. 
`interview-squashed` will only show final version.

How To Run
===========================
- sbt run 
- open http://localhost:8888/API?from=USD&to=AUD in browser

Future Work 
===========================
- Use/Implement cache with better Monix Task Support 
- Improve performance in case of cache miss. 
  Currently on cache miss we are fetching conversion rates for all valid currency pairs, which might be slower then the convert api.
- Better Error Handling

