
To start:
open elastic search, master true, comment out host, and zen 
open kibana
open index.html
type 1 of the 3 queries
click on the score, then next, after rating 0-199 files, 
type 2 of the 3 queries 
repeat  

results are stored in elastic search.

Usage:
Change the config.js file rateBy : your name

ElasticSearch mapping

PUT /rating/
{
  "settings": {
    "index": {
      "store": {
        "type": "fs"
      },
      "number_of_shards": 1,
      "number_of_replicas": 0
    }
  }
}
PUT /rating/document/_mapping
{
  "properties": { 
      "docno": { 
        "type": "text", 
        "store": true, 
        "term_vector": "with_positions_offsets_payloads" 
      }, 
      "query":{ 
        "type": "text", 
        "store": true 
      },
      "ratedby":{ 
        "type": "text", 
        "store": true 
      },
      "rate": { 
        "type": "integer", 
        "store": true 
      }
  }
}