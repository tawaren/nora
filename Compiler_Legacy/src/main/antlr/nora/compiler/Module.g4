grammar Module;

@header {
    package nora.compiler;
}

file_def: import_def* module_def;
import_def: IMPORT import_path;
import_path : reference ('.' star='*')?;
module_def: MODULE id '{' definition* '}';
definition : type_def | trait_def | /*marking_def |*/ multi_method_def | case_method_def | function_def;
traits : parametric_ref (',' parametric_ref)*;
type_def: layout_handler_annotation? type_id_annotation? (SEALED? ABSTRACT)? DATA id generics_def? (EXTENDS super=parametric_ref)? (WITH traits)? ('{'
    (argument_def (';' argument_def)* ';'?)?
'}')?;
type_id_annotation: TYPE_ID '(' int ')' ;
layout_handler_annotation: LAYOUT_HANDLER '(' id ')' ;

trait_def: TRAIT id generics_def? (EXTENDS traits)?;
//marking_def: MARK reference WITH instances;
multi_method_def: PARTIAL? SEALED? METH id generics_hint_def? '(' dynamic=arguments_def? ('|' static=arguments_def)? ')' ':' parametric_ref;
generics_def: '[' (variant_generic_def (',' variant_generic_def)*)? ']';
variant_generic_def: variance? generic_def;
generic_def: id (':' parametric_ref)?;

variance: INV | ADD | SUB;
//These two are only in the syntax, not yet in the semantics
generics_hint_def:  '[' (generic_hint_def (',' generic_hint_def)*)? ']';
generic_hint_def: generic_def ('=' parametric_ref)?;
arguments_def: argument_def (',' argument_def)*;
argument_def: id ':' parametric_ref;
case_method_def : case_annotations METH CASE generics_hint_def? multi=parametric_ref '(' dynamic=arguments_def? (('|' static=arguments_def) | '|')? ')' (':' ret=parametric_ref)? '=' expr
                | case_annotations PRIMITIVE '(' prim=id ')' METH CASE generics_hint_def? multi=parametric_ref '(' dynamic=arguments_def? (('|' static=arguments_def) | '|')? ')' ':' ret=parametric_ref;
case_annotations: position_annotation? name_annotation?;
position_annotation: (BEFORE | AFTER) '(' reference ')' ;
name_annotation: NAMED '(' id ')' ;

function_def : FUN name=id generics_hint_def? '(' arguments_def? ')' (':' parametric_ref)? '=' expr
             | PRIMITIVE '(' prim=id ')' FUN name=id generics_hint_def? '(' arguments_def? ')' ':' parametric_ref?;
type_appl : '[' (parametric_ref (',' parametric_ref)*)? ']';
parametric_ref : funRef | tupleRef | plainRef;
plainRef : reference type_appl?;
funRef : '(' parametric_ref (',' parametric_ref)* ')' '=>' parametric_ref;
tupleRef : '(' parametric_ref (',' parametric_ref)+ ')';

expr: expr ':' parametric_ref
    | prio9
    ;

prio9: op1=prio9 OR op2=prio8
     | prio8
     ;

prio8: op1=prio8 XOR op2=prio7
     | prio7
     ;

prio7: op1=prio7 AMP op2=prio6
     | prio6
     ;

prio6: op1=prio6 (EQ EQ | BANG EQ) op2=prio5
     | prio5
     ;

prio5: op1=prio5 (LT | LT EQ | GT | GT EQ) op2=prio4
     | prio4
     ;

prio4: op1=prio4 (ADD | SUB) op2=prio3
     | prio3
     ;
prio3: op1=prio3 (MUL | DIV | MOD) op2=prio2
     | prio2
     ;

prio2: (BANG | INV ) op1=prio2
     | prio1
     ;

prio1: prio1 '->' id
     | prio0
     ;

prio0: instanceOf | call | create | tuple | let | if | scope;

instanceOf : scope IS parametric_ref;
call : scope '(' exprs? ')';
let: LET id (':' parametric_ref)? '=' val=expr IN cont=expr;
if: IF cond=expr THEN then=expr ELSE other=expr;
create: parametric_ref '{' named_exprs? '}';
tuple: '(' expr (',' expr)+ ')';

scope: baseExpr
     | '(' expr ')'
     | '{' expr '}'
     ;

baseExpr: closure | symbol | lit ;

exprs: expr (',' expr)*;
named_exprs : named_expr (',' named_expr)*;
named_expr: (id '=')? expr;

closure : '\\' closure_argument_def? (',' closure_argument_def)* '->' expr;
closure_argument_def: id (':' parametric_ref)?;

symbol: plainRef;
lit : int | STRING | BOOL;
reference : SELF | id ('.' id)*;
id : TICK? v1=ID | TICK v2=keyword;
int : (ADD | SUB)? NUM;
keyword : raw=IMPORT | raw=MODULE | raw=ABSTRACT | raw=DATA | raw=EXTENDS | raw=WITH | raw=TRAIT | raw=MARK | raw=FUN | raw=METH | raw=CASE | raw=PARTIAL | raw=SEALED | raw=BOOL | raw=LET | raw=IN | raw=IF | raw=THEN | raw=ELSE | raw=IS;

BLOCK_COMMENT : '/*' .*? ( '*/' | EOF ) -> channel(HIDDEN) ;
LINE_COMMENT: '//' ~[\r\n]* -> channel(HIDDEN) ;
WS  : [ \t\r\n]+ -> skip ;

IMPORT: 'import';
MODULE: 'module';
ABSTRACT: 'abstract';
SEALED: 'sealed';
DATA: 'data';
EXTENDS: 'extends';
WITH: 'with';
TRAIT: 'trait';
MARK: 'mark';
PARTIAL: 'partial';
FUN: 'function';
METH: 'method';
CASE: 'case';
SELF: 'self';
TYPE_ID: '@TypeId';
LAYOUT_HANDLER: '@LayoutHandler';
NAMED: '@Named';
TAIL_REC_OPTIM: '@EnableTailRecursion';
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
