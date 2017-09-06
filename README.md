# WebCrawler-SearchEngine

A web cralwer using java, breadth first algorithm, with ElasticSearch backend and Search Engine front end

TechStacks: __Java__, __Javascript__, __Node.js__, __HTML__, __CSS__

### Objective
Team of three, each individual will crawl using __3__ seed URLs:
* one of the URLs provided to the team, 
* at least two additional seed URLs you devise on your own. 
* In total, the members of your team will crawl from at least nine seed URLs.
* Each individual crawl 20,000 unique links and merge into about 50,000 links


### Crawling
##### Politeness Policy
The crawler strictly observe this politeness policy at all times, including during development and testing. 

* Make no more than one HTTP request per second from any given domain. The simplest approach is to make one request at a time and have your program sleep between requests. 
* Before crawl the first page from a given domain, fetch its robots.txt file and make sure crawler strictly obeys the file. 

__Problems and Improvements:__ 

Improved time efficiency with hashtable.
_________________________________________________________________________________

##### Frontier Management, Breadth First Search with Priority Queue
* The frontier is a queue like datastrucure. For each page, the frontier stores the canonicalized page URL and the in-link count to the page from other pages you have already crawled. 
* When selecting the next page to crawl, the next page is chosen in the following order:
  * Seed URLs should always be crawled first.
  * Prefer pages with higher in-link counts.
  * If multiple pages have maximal in-link counts, choose the option which has been in the queue the longest.
 
__Problems and Improvements:__ 

Improved time efficiency with hashtable.
_________________________________________________________________________________


##### URL Canonicalization
Many URLs can refer to the same web resource. In order to ensure that you crawl 20,000 distinct web sites, we made a canonicalizer with the following rules.
* Convert the scheme and host to lower case: HTTP://www.Example.com/SomeFile.html → http://www.example.com/SomeFile.html
* Remove port 80 from http URLs, and port 443 from HTTPS URLs: http://www.example.com:80 → http://www.example.com
* Make relative URLs absolute: if crawl both http://www.example.com/a/b.html and find the URL ../c.html, it should canonicalize to http://www.example.com/c.html.
* Remove the fragment, which begins with #: http://www.example.com/a.html#anything → http://www.example.com/a.html
* Remove duplicate slashes: http://www.example.com//a.html → http://www.example.com/a.html
_________________________________________________________________________________
##### Document Processing
* Extract all links in link tags. 
* Canonicalize the URL, add it to the frontier if it has not been crawled (or increment the in-link count if the URL is already in the frontier), and record it as an out-link in the link graph file.
* Extract the document text, stripped of all HTML formatting, JavaScript, CSS, and so on. Write the document text to a file in the same format as the AP89 corpus, as described below. 
* Use the canonical URL as the DOCNO. If the page has a title tag, store its contents in a HEAD element in the file. 
* Store the entire HTTP response separately, as described below.

__Problems and Improvements:__ 

Improved time efficiency with hashtable.

__________________________________________________________________________________

### Merging into Elastic Search
Merge all cralwed data, with docno, title, text, html raw text, headers, and author into elastic search with same team index. 

__Problems and Improvements:__ 

Improved time efficiency with hashtable.

__________________________________________________________________________________

### Link Graph

Store a link graph of all nodes = canonicalized url, inlinkes and outlinks of all the crawled pages.


__Problems and Improvements:__ 

Improved time efficiency with hashtable.

__________________________________________________________________________________


### Vertical Search and Evaluation

* Add all 50,000 documents to an elasticsearch index, using the canonical URL as the document ID for de-duplication, and create a simple HTML page which runs queries against your elasticsearch index.

* Use Calaca to create a  search engine that allow users to enter text queries, and display elasticsearch results to those queries from your index. The result list should contain at minimum the URL to the page you crawled.

[img](Interface/img.png)

__Problems and Improvements:__ 

Improved time efficiency with hashtable.

__________________________________________________________________________________

