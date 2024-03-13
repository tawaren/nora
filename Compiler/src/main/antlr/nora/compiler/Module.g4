grammar Module;

@header {
    package nora.compiler;
}

file_def: import_def* module_def;
import_def: IMPORT (reference | path ('::' star='*')?);

module_def: annotation* MODULE id '{' definition* '}';
definition : type_def | data_def | trait_def | multi_method_def | case_method_def | function_def;

type_def : annotation* full_modifier* TYPE id generics_def? (WITH traits)?
         | annotation* full_modifier* TYPE id '#' value_ref generics_def? (WITH traits)?
         ;

data_def: annotation* access_modifier* DATA id generics_def? (WITH traits)? data_body?
        | annotation* access_modifier* DATA id '#' value_ref generics_def? data_body?
        ;

data_body : '{' (argument_def (';' argument_def)* ';'?)? '}';

trait_def: annotation* full_modifier* TRAIT id generics_def? (EXTENDS traits)?;

multi_method_def: annotation* full_modifier* METH id generics_hint_def? '(' dynamic=arguments_def? ('|' static=arguments_def)? ')' ':' parametric_ref;

case_method_def : annotation* access_modifier* FUN id generics_hint_def? '#' parametric_value_ref '(' arguments_def? ')' (':' parametric_ref)? '=' expr;

function_def : annotation* access_modifier* FUN name=id generics_hint_def? '(' arguments_def? ')' (':' parametric_ref)? '=' expr;

annotation: '@' value_ref ('(' (annotation_value (',' annotation_value)*)? ')')?;
annotation_value: id EQ (lit | parametric_ref);

access_modifier: PUBLIC | PRIVATE;
full_modifier: access_modifier | SEALED;

traits : parametric_ref (',' parametric_ref)*;
generics_def: '[' (variant_generic_def (',' variant_generic_def)*)? ']';
variant_generic_def: variance? generic_def;
generic_def: id (':' bounds)?;
bounds: parametric_ref (',' parametric_ref)*;
variance: INV | ADD | SUB;
generics_hint_def:  '[' (generic_hint_def (',' generic_hint_def)*)? ']';
generic_hint_def: generic_def ('=' parametric_ref)?;
arguments_def: argument_def (',' argument_def)*;
argument_def: id ':' parametric_ref;

parametric_ref : funRef | tupleRef | parametric_value_ref;
parametric_value_ref : value_ref type_appl?;
value_ref : reference | id;

funRef : '(' parametric_ref (',' parametric_ref)* ')' '=>' parametric_ref;
tupleRef : '(' parametric_ref (',' parametric_ref)+ ')';
type_appl : '[' (parametric_ref (',' parametric_ref)*)? ']';

expr: type_hint_expr;

type_hint_expr : type_hint_expr ':' parametric_ref
               | instanceOf_expr
               ;

instanceOf_expr : instanceOf_expr IS parametric_ref
               | or_expr
               ;

or_expr: op1=or_expr OR op2=xor_expr
       | xor_expr
       ;

xor_expr: op1=xor_expr XOR op2=and_expr
        | and_expr
        ;

and_expr: op1=and_expr AMP op2=eq_expr
        | eq_expr
        ;

eq_expr: op1=eq_expr (EQ EQ | BANG EQ) op2=comp_expr
       | comp_expr
       ;

comp_expr: op1=comp_expr (LT | LT EQ | GT | GT EQ) op2=add_expr
         | add_expr
         ;

add_expr: op1=add_expr (ADD | SUB) op2=mul_expr
        | mul_expr
        ;

mul_expr: op1=mul_expr (MUL | DIV | MOD) op2=not_expr
        | not_expr
        ;

not_expr: (BANG | INV ) op1=not_expr
        | field_expr
        ;

field_expr: field_expr '.' id
     | struct_expr
     ;

struct_expr: call | create | tuple | let | if | scope;
call : scope '(' exprs? ')';

scope: baseExpr
     | '(' expr ')'
     | '{' expr '}'
     ;

baseExpr: closure | ref_expr | lit ;
ref_expr : parametric_value_ref | SELF | id ;

let: LET id (':' parametric_ref)? '=' val=expr IN cont=expr;
if: IF cond=expr THEN then=expr ELSE other=expr;
create: parametric_value_ref '{' named_exprs? '}';
tuple: '(' expr (',' expr)+ ')';

exprs: expr (',' expr)*;
named_exprs : named_expr (',' named_expr)*;
named_expr: (id '=')? expr;

closure : '\\' closure_argument_def? (',' closure_argument_def)* '->' expr;
closure_argument_def: id (':' parametric_ref)?;

lit : int | STRING | BOOL;

reference : path '::' id;
path : id ('.' id)*;

id : TICK? v1=ID | TICK v2=keyword;
int : (ADD | SUB)? NUM;
keyword : raw=IMPORT | raw=MODULE | raw=TYPE | raw=DATA | raw=EXTENDS | raw=WITH | raw=TRAIT | raw=PUBLIC | raw=PRIVATE | raw=METH | raw=FUN | raw=SEALED | raw=BOOL | raw=LET | raw=IN | raw=IF | raw=THEN | raw=ELSE | raw=IS;

BLOCK_COMMENT : '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN) ;
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN) ;
WS  : [ \t\r\n]+ -> skip ;

TYPE: 'type';
PUBLIC: 'public';
PRIVATE: 'private';
IMPORT: 'import';
MODULE: 'module';
SEALED: 'sealed';
DATA: 'data';
EXTENDS: 'extends';
WITH: 'with';
TRAIT: 'trait';
FUN: 'function';
METH: 'method';
SELF: 'self';
TYPE_ID: '@TypeId';
LAYOUT_HANDLER: '@LayoutHandler';
NAMED: '@Named';
PRIMITIVE: '@Primitive';
BEFORE: '@Before';
AFTER: '@After';

AMP: '&';
BANG: '!';
INV: '~';
OR: '|';
XOR: '^';
MUL: '*';
DIV: '/';
MOD: '%';
ADD: '+';
SUB: '-';
EQ: '=';
LT: '<';
GT: '>';
LET: 'let';
IN: 'in';
IF: 'if';
THEN: 'then';
ELSE: 'else';
IS: 'is';

TICK: '`';
STRING: '"' ( ESC_SEQ | ~["\\] )* '"' ;
ID : ALPHA (ALPHA|DIGIT)*;
NUM : INT;
BOOL : 'true' | 'false';

fragment ALPHA: [_a-zA-Z];
fragment DIGIT: [0-9] ;
fragment INT : DIGIT+ ;
fragment ESC_SEQ : '\\' [btnfr"'\\];
