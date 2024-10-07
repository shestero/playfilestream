# playfilestream

How to get file data stream from multipart body in Play3.

To process input file as stream you are to override

```
def process(file: FileBodyPart, fields: Map[String, String]): Future[Result]
```

When the method will also receive all field values that were before the file part in the body.


The **MultipartBodyParser** expore following assumptions:

1. There is only one file in the form (at least it stops after the first file).
2. The uploaded file is plan text file and is uploaded as text/plain.
3. Lines (separated by *\n*) - as in field values as in the uploaded file - are no longer than maxStringLen=4096 including *\r*.
4. There are no empty lines in the uploaded file (if the lines in it are separated by *\r\n*) either there are no lines with only char *\r* (if the lines are separated by *\n*).

See also:

1. [https://github.com/playframework/playframework/issues/9852](https://https://github.com/playframework/playframework/issues/9852)
2. [https://github.com/playframework/playframework/pull/10309](https://github.com/playframework/playframework/pull/10309)
3. [Multipart input file streaming in Akka-Http](https://github.com/shestero/formfile)
