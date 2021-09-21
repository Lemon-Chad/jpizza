# JPizza Extension Development

So, you want to make an extension for JPizza? Well, You've come to the right place.

## Getting Started

The first thing you need to do is [clone the base extension repo.](https://github.com/Lemon-Chad/JPizza-Extension-Template)
This repo contains all of the code and gradle magic to make your own JPizza extension.

After you have the code, Pick a name for your extension. Should be the easy part.

## Beginning Development

In order to start writing code for your extension, look in the `app/src/main/java/jpext` folder. In there, there should be one file, named `LibraryName.java` (You should rename this to match the name of your library)

Once you have the file opened, make sure to change every instance of LibraryName to the exact same thing you put as the name of the file. Now, you are almost ready to start making all of your custom functions!

Next, you need the JPizza jar. Download the latest jar from github, (or compile it yourself), and put it into the `app/jars` folder. Make sure to name it jpizza-all.jar

This is necessary for all the imports to work, but don't worry, it's not included in the final jar of your extension.

## Making Libraries

So, you want to make a function, but where does that function go?

Well, in a library of course!

If you look in the LibraryName.java (Which is what it will be referred to, even though you should have changed the name), you will see a function called `initialize`. This is where all of your initialization code should go. To make the initialization code, simply make a call to the initialize function with the name of your import, the class of your library, (as in, `LibraryName.class`), and finally, a HashMap. In this HashMap, you should `put` all of your functions that will go into that library and a list of parameters for the function.
Example:
`initialize("demo", LibraryName.class, new HashMap<>(){{  
    put("printDemo", Collections.singletonList("value"));  
}});`
This is the initialize call for a library called demo, with one function, 'printDemo', which takes in 1 'value' as its parameter.

## Making Functions

So, you have your libraries, but how do you make functions? Well, once you add it to your library, just create a public Java function below with a return type of RTResult. Make sure to make the name of the function `execute_FunctionName`. This function should take in 1 value, a Context. From this, just do what you gotta do, add a return somewhere, and bam. Easy extension done!

## What Next?

So you have all your code, but how do you use this? Well, just run the gradle task named `jar`. You can do this by running `./gradlew jar` on unix or `./gradlew.bat jar` on windows.

If everything is good, this should create a jar in the app/build/libs folder called `app.jar`. Simply rename this to the name of your extension, (as used previously),  and add it to your `.jpizza/extensions` folder located in your home directory (unless you configured otherwise). Then, simply load up JPizza, and use the command `extend LibraryName` to load the extension, then just import anything you want to use!

### Notes
- This is a VERY new and untested system. If you find any bugs with the extension template/compilation process, Please create an issue on the template's github repository. If you find any bugs with the JPizza command, Please create an issue on JPizza's github repository.
- This system will be better eventually once we know what bugs there are. You can use this to do anything you can do with Java within JPizza, so we want it to be as good as possible.
