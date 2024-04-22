# Compile the source code
javac -d bin -sourcepath src src/myfiles/FileSystemServer.java

# Create the JAR file
jar cvfm dist/FileSystemServer.jar MANIFEST.MF -C bin/ .
