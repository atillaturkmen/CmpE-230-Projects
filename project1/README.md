# myLang-to-LLVM
This is a project from my Systems Programming class in second year.
Takes basic instructions written like C code for input. Returns low level virtual machine code that can be compiled (with clang) or directly run with lli command.
Lli version 3.3 is needed. Can be downloaded for ubuntu from this link: https://releases.llvm.org/download.html#3.3
Type `make` to build the project. Enter `java -jar ./mylang2ir testcases/input.my` to translate the file in testcases/input.my. input.ll file will be created in the same directory. Type `lli testcases/input.ll` to see the output of generated LLVM code.
