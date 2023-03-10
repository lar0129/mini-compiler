lexer grammar SysYLexer;

CONST : 'const';

INT : 'int';

VOID : 'void';

IF : 'if';

ELSE : 'else';

WHILE : 'while';

BREAK : 'break';

CONTINUE : 'continue';

RETURN : 'return';

PLUS : '+';

MINUS : '-';

MUL : '*';

DIV : '/';

MOD : '%';

ASSIGN : '=';

EQ : '==';

NEQ : '!=';

LT : '<';

GT : '>';

LE : '<=';

GE : '>=';

NOT : '!';

AND : '&&';

OR : '||';

L_PAREN : '(';

R_PAREN : ')';

L_BRACE : '{';

R_BRACE : '}';

L_BRACKT : '[';

R_BRACKT : ']';

COMMA : ',';

SEMICOLON : ';';

IDENT :
//      以下划线或字母开头，仅包含下划线、英文字母大小写、阿拉伯数字
       [a-zA-Z_][a-zA-Z0-9_]*;

INTEGER_CONST :
//      数字常量，包含十进制数，0开头的八进制数，0x或0X开头的十六进制数
        TEN_INTEGER | EIGHT_INTEGER | HEX_INTEGER;
fragment TEN_INTEGER: '0'|[1-9][0-9]*;
fragment EIGHT_INTEGER: '0'+[1-7]+;
fragment HEX_INTEGER: ('0x'| '0X')[0-9A-Fa-f]+;

WS
   : [ \r\n\t]+ ->skip
   ;

LINE_COMMENT
   : '//' .*? '\n' ->skip
   ;

MULTILINE_COMMENT
   : '/*' .*? '*/' ->skip
   ;
