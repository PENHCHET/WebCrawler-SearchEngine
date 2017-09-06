/*
 * Calaca - Search UI for Elasticsearch
 * https://github.com/romansanchez/Calaca
 * http://romansanchez.me
 * @rooomansanchez
 * 
 * v1.2.0
 * MIT License
 */

/* Service to Elasticsearch */
Calaca.factory('calacaService', ['$q', 'esFactory', '$location', function($q, elasticsearch, $location){

    //Set default url if not configured
    CALACA_CONFIGS.url = (CALACA_CONFIGS.url.length > 0)  ? CALACA_CONFIGS.url : $location.protocol() + '://' +$location.host() + ":9200";
	var upload = function(docno, score, queryString) {
		var uploadClient = elasticsearch({ host: HW5_CONFIGS.url});
		uploadClient.create({
			"index": HW5_CONFIGS.index_name,
			"type": HW5_CONFIGS.type,
			"id": docno,
			"body": {
				"docno": docno,
				"query" : queryString,
				"rateby": HW5_CONFIGS.rateBy,
				"rate": score
			}
		}, function (error, response) {
			//already exist, update 
			//console.log(error);
			//console.log(response);
		});
	}
	var update = function(docno, score, queryString) {
		var uploadClient = elasticsearch({ host: HW5_CONFIGS.url});
		uploadClient.update({
			"index": HW5_CONFIGS.index_name,
			"type": HW5_CONFIGS.type,
			"id": docno,
			"body": {
				"rateby": HW5_CONFIGS.rateBy,
				"rate": score,
				"query" : queryString
			}
		}, function (error, response) {
			//console.log(error);
			//nothing to update, then create new 
			upload(docno, score, queryString);
		});
	}
    var client = elasticsearch({ host: CALACA_CONFIGS.url });
    var search = function(query, mode, offset){
        var deferred = $q.defer();

        if (query.length == 0) {
            deferred.resolve({ timeTook: 0, hitsCount: 0, hits: [] });
            return deferred.promise;
        }

        client.search({
                "index": CALACA_CONFIGS.index_name,
                "type": CALACA_CONFIGS.type,
                "body": {
                    "size": CALACA_CONFIGS.size,
                    "from": offset,
                    "query": {
                        "query_string": {
                            "query": query
                        }
                    }
                }
        }).then(function(result) {

                var i = 0, hitsIn, hitsOut = [], source;
                hitsIn = (result.hits || {}).hits || [];
                for(;i < hitsIn.length; i++){
                    source = hitsIn[i]._source;
                    source._id = hitsIn[i]._id;
                    source._index = hitsIn[i]._index;
                    source._type = hitsIn[i]._type;
                    source._score = hitsIn[i]._score;
                    hitsOut.push(source);
                }
                deferred.resolve({ timeTook: result.took, hitsCount: result.hits.total, hits: hitsOut });
        }, deferred.reject);

        return deferred.promise;
    };

    return {
        "search": search,
		"upload": upload,
		"update": update
    };

    }]
);
