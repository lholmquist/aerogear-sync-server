## Synchronizers module
A synchronizer is tied to a specific document/object content type, and also a specific patching algorithm. 
One synchronizer might support plain text while another supports JSON Objects as the content type of documents. 
Different patch algorithms are suitable for different document types.

### Modules
* [diffmatchpatch](./diffmatchpatch)  
This implementation uses plain _String_s as the document data type and Neil Frasers DiffMatchPatch Java implementation 
for its patching algorithm.

* [json-patch](./json-patch)  
This implementation uses [Jackson](http://wiki.fasterxml.com/JacksonHome) _JsonNode_ as the document data type and uses 
[JSON PATCH](https://tools.ietf.org/html/rfc6902) for its patching algorithm.
 
* [json-merge-patch](./json-merge-patch)  
This implementation uses [Jackson](http://wiki.fasterxml.com/JacksonHome) _JsonNode_ as the document data type and uses 
[JSON Merge](http://tools.ietf.org/html/rfc7386) for its patching algorithm.



