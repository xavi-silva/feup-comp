grammar Javamm;

@header {
    package pt.up.fe.comp2025;
}

CLASS : 'class' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
PONTINHOS: '...';
VARARGS : INT (' ')? PONTINHOS ;
INTEGER : [0-9]+ ;
ID : [a-zA-Z_$][a-zA-Z0-9_$]* ;

SINGLE_COMMENT : '//' .*? '\n' -> skip ;
MULTI_COMMENT : '/*' .*? '*/' -> skip ;
WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : 'import' name+=ID ('.' name+=ID)* ';' #Import
    ;

classDecl
    : CLASS name=ID ('extends' superName=ID)?
        '{'
        ( varDecl )*
        ( methodDecl )*
        '}'
    ;

varDecl
    : type name=ID ';'
    ;

type
    : type'[' ']' #Array
    | dataType = VARARGS #VarArgs
    | dataType = INT #Int
    | dataType = 'boolean' #Boolean
    | dataType = ID #Id
    ;


methodDecl locals[boolean isPublic=false, boolean isStatic=false]
    : ((public_=PUBLIC {$isPublic=true;})?
        type name=ID
        '(' (param (',' param)*)? ')'
        '{' varDecl* stmt* returnStmt '}')
    | ((public_=PUBLIC {$isPublic=true;})?
        'static' {$isStatic=true;} void_='void' name=ID '(' ID '['']' paramName=ID ')'
        '{' varDecl* stmt* '}')
    ;

param
    : type name=ID
    ;

stmt
    : '{' ( stmt )* '}' #BlockStmt
    | 'if' '(' expr ')' stmt 'else' stmt #IfStmt
    | 'while' '(' expr ')' stmt #LoopStmt
    | expr ';' #SimpleStmt
    | name=ID '=' expr ';' #AssignStmt
    | name=ID '[' expr ']' '=' expr ';' #ArrayStmt
    ;

returnStmt
        : RETURN expr ';' #ReturnStatement
        ;

expr
    : '(' expr ')' #ParenExpr
    | 'new' name=ID '('')'  #NewExpr
    | 'new' (INT'[' expr ']') #NewArrayExpr
    | op='!' expr #UnaryExpr
    | expr '[' expr ']' #ArrayAcessExpr
    | expr op=('*' | '/' ) expr #BinaryExpr
    | expr op=('+' | '-') expr #BinaryExpr
    | expr op='<' expr #BinaryExpr
    | expr op='&&' expr #BinaryExpr
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    | expr '.' name=ID '(' (expr (',' expr)*)? ')' #MethodCallExpr
    | '[' (expr (',' expr)*)? ']' #ArrayExpr
    | expr '.' name=ID #LengthExpr
    | value='true' #Identifier
    | value='false' #Identifier
    | value='this' #ThisExpr
    ;


