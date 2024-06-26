include Makefile.git

export CLASSPATH=/usr/local/lib/antlr-*-complete.jar

DOMAINNAME = 47.122.3.40:3000
ANTLR = java -jar /usr/local/lib/antlr-*-complete.jar -listener -visitor -long-messages
JAVAC = javac -g
JAVA = java


PFILE = $(shell find . -name "SysYParser.g4")
LFILE = $(shell find . -name "SysYLexer.g4")
JAVAFILE = $(shell find . -name "*.java")
ANTLRPATH = $(shell find /usr/local/lib -name "antlr-*-complete.jar")

compile: antlr
	$(call git_commit,"make")
	mkdir -p classes
#	$(JAVAC) $(JAVAFILE) -d classes
	$(JAVAC) -classpath $(ANTLRPATH) $(JAVAFILE) -d classes

run: compile
	java -classpath ./classes:$(ANTLRPATH) Main $(FILEPATH)


antlr: $(LFILE) $(PFILE)
	$(call git_commit, "antlr")
	$(ANTLR) $(PFILE) $(LFILE)


test: compile
	$(call git_commit, "test")
	: > nohup.out
	nohup java -classpath ./classes:$(ANTLRPATH) Main ./tests/test1.sysy &
	cp nohup.out ./tests/

gen:
	clang -S -emit-llvm ./tests/test.c -o ./tests/testc.ll -O0

testout:
	llvm-as tests/test.ll
	llc ./tests/test.bc -o result.s
	gcc result.s -o result
#	bash ./result
#	echo $?


clean:
	rm -f src/main/java/*.tokens
	rm -f src/*.interp
	rm -f src/SysYLexer.java src/SysYParser.java src/SysYParserBaseListener.java src/SysYParserBaseVisitor.java src/SysYParserListener.java src/SysYParserVisitor.java
	rm -rf classes


submit: clean
	git gc
	bash submit.sh


.PHONY: compile antlr test run clean submit


