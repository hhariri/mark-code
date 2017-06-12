## Mark-Code

Mark-Code is a command line application that can parse AsciiDoc (Markdown support coming) files for source code, extract the code, compile it and make sure everything works. Its main purpose is for those that 
write documentation for API's et al and it in some way helps make sure that the code in documentation is current (or at least compiles).

Currently it mainly supports Kotlin with some features available for Java. For now `mark-code` only supports [AsciiDoc](http://asciidoctor.org) and in general supports all the syntax available for [code snippets for AsciiDoc](http://asciidoctor.org/docs/asciidoc-writers-guide/#listing-and-source-code-blocks).
                                                                         
## What does mark-code do?

It does two things:

* Extract all source code from your documentation.
* Compiles and optionally runs the verifier to make sure the output matches the input. 

## Creating Code Snippets in your Documentation

### Output Structure

When `mark-code` processes your input files (*.adoc), it creates a folder for each `chapter`, which is defined with `==` in AsciiDoc. Inside the folder
a Kotlin file for each individual snippet. 


### Source code

Any source code you write will automatically be encapsulated in a `main` function (except of course the `main` function). In other words

```asciidoc
[source,kotlin]
----
fun sum(x: Int, y: Int) = x + y
sum(2,3)
----
```

would generate

```kotlin
fun sum(x: Int, y: Int) = x + y

fun main(args: Array<String>) {
    sum(2,3)
}
```

whereas 

```asciidoc
[source,kotlin]
----
package org.kotlin.byexample

fun main(args: Array<String>) { 
    println("Hello, World!") 
}
----
```

would generate

```kotlin
package org.kotlin.byexample

fun main(args: Array<String>) {
    println("Hello, World!")
}
```

Like all AsciiDoc source, you can annotate your code

```asciidoc
[source,kotlin]
----
package org.kotlin.byexample <1>

fun main(args: Array<String>) { <2>
    println("Hello, World!") <3>
}
----
<1> Kotlin code is usually defined in packages. If you don't define one, the default package will be used
<2> The main entry point to a Kotlin application is a function called *main*
<3> `println` writes to standard output and is implicitly imported
```

### Code snippet

If you want `mark-code` to ignore a Kotlin code snippet, yet still have it in your document, use `kotlin-snippet`

```asciidoc
[source,kotlin-snippet]
----
fun sum(x: Int, y: Int) = x + y
----
```
### Referencing Code Snippets

You can give code snippets names that you can later reference

```asciidoc
[source,kotlin,Point.kt]
----
data class Point(val x: Int, val y: Int)
----
```

and include it in another snippet
 
 ```asciidoc
[source,kotlin,prepend=Point.kt]
----
val point = Point(20, 30)
----
```

### Define main function

You can define a main function for a code snippet, different to the default `main`

 ```asciidoc
[source,kotlin,main=ExecuteFile.kt]
----
val point = Point(20, 30)
----
```

### Verify output

You can execute a code snippet and validate the output (currently works for standard output)

```asciidoc
[source,kotlin]
----
fun sum(x: Int, y: Int) = x + y

>>> println(sum(3, 6))
9
```

What you want executed you prefix with `>>>` and on the next line you put the result

## How to use it

The easiest way is to run the Gradle task `distZip`. This create a zip file in the `distributions` folder which contains a few files and folders. 

Unzip the contents to your project folder, where you'd call the tool located in the `bin` folder `mark-code` from. In order for this to work, you need
 to make sure that the following files are located in your project folder:
 
* `imports.txt` and `prefixes.txt`: should be placed in the root folder of your project. Copy them from the contents of the `zip` file and modify at will.
* `OutputVerifier.kt`: copy it to the source folder which is usually where the `VerifyAllSamples.kt` will be located

To invoke the tool, use the following command:
  
```bash
mark-code <sources> <output> -o
```  

where <sources> is where your *.adoc (*.md not supported yet) are located, and <output> is where you want the code to be verified to be placed. The -o is optional and generates the
`VerifyAllSamples.kt` which actually executes the code to see if the output is as specified.

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
 

## License

Project is Licensed under MIT (c) 2017 Hadi Hariri and Contributors


