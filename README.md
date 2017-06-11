## Mark-Code

Mark-Code is a command line application that can parse AsciiDoc (Markdown support coming) files for source code, extract the code, compile it and make sure everything works. It's main purpose is for those that 
write documentation for API's et al and it in some way helps make sure that the code in documentation is current (or at least compiles).

## TODO

* Add unit tests as adding/refactoring
* Continue code reorganisation and cleanup
* Make it independent of concept of Chapters
* Add markdown support

Contributions welcome.


## To run

Run the Gradle `distZip` task. Copy the output to your project folder (usually `tools`). Copy the
imports.txt and prefixes.txt samples to your root folder and modify as needed.
Copy the src/com/hadihariri/markcode/OutputVerifier.kt to your -o folder. 
 
## Credits

Credits to [Dimitry Jemerov](https://twitter.com/intelliyole) for his work on this. Dmitry wrote the original code for the [Kotlin In Action](https://www.manning.com/books/kotlin-in-action) book.
 


