# Parse a file part of multi-part form data stream directly to a stream

Demo project: how to get file data stream from multipart body in Play3.

To process input file as stream you are to override:

```
def process(file: FileBodyPart, fields: Map[String, String]): Future[Result]
```

When the method will also get all field values that were sent before the file part in the body.
Result may be an output steam or a value.


The **MultipartBodyParser** is a demo code that expores the following assumptions:

1. There is only one file in the form (at least it takes only the first file part).
2. All uploaded files are plan text files and they are uploaded as text/plain data.
3. Lines (separated by *\n*) - both inside field values and in the uploaded file content - are no longer than maxStringLen=4096 bytes (including trailing *\r* if present).
4. There are no empty lines inside the uploaded file (if we count lines separated by *\r\n*) either there are no lines with only char *\r* (if the lines are separated by *\n* only).

See also:

1. [https://github.com/playframework/playframework/issues/9852](https://github.com/playframework/playframework/issues/9852)
2. [https://github.com/playframework/playframework/pull/10309](https://github.com/playframework/playframework/pull/10309)
3. [Multipart input file streaming in Akka-Http](https://github.com/shestero/formfile)
