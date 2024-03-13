grammar Method;

@header {
    package nora.runtime;
}

method_def: WS*
    LOCALS ':' WS* loc=NUM WS+
    ((RECURSION | CTX_RECURSION) ':' WS* rec=NUM WS+)?
    SIG ':' WS* '(' (WS* type_info (WS* ',' WS* type_info)*)? WS* ')' WS* '->' WS* type_info WS+
    BODY ':' WS* expr WS*
    EOF;

expr : WS* '(' WS* expr0 WS* ')' WS*
     | WS* expr0 WS*;

expr0 : block
      | slot
      | arg
      | capt
      | targ
      | lit
      | create
      | field
      | ifThenElse
      | subTypeOf
      | typeOf
      | method
      | multi_method
      | closure
      | call
      | tail_rec
      | primitive;

block : '{' WS* (let WS* ';' WS*)* expr '}';
let : LET WS* slot WS* '=' expr;
lit : (BYTENOTE | INTNOTE | NUMNOTE)? NUM | STRING | BOOLEAN;
create : (CREATE | TAILCREATE) WS* '#' WS* entity_name WS* generics WS* arguments;
field : FIELD  WS* '#' WS* entity_name WS* generics '(' expr ',' WS* NUM WS* ')';
ifThenElse: IF typeHint cond=expr THEN then=expr ELSE other=expr;
typeHint: WS* '[' WS* targ WS* ']' WS*;
subTypeOf : SUB_TYPE_OF WS* '(' val=expr ',' typ=expr ')';
typeOf : TYPE_OF WS* '(' val=expr ')';
method : METHOD WS* '#' WS* entity_name WS* generics;
multi_method : MULTI_METHOD WS* '#' WS* entity_name WS* generics;
closure : CLOSURE WS* generics WS* arguments WS* '[' targ ']' WS* '{' WS* NUM WS* '}' expr;
generics : '[' (expr (',' expr)*)? WS* ']';
arguments : '(' (expr (',' expr)*)? WS* ')';
primitive: PRIMITIVE WS* '#' WS* id WS* arguments;
call : CALL expr arguments;
tail_rec: TAILREC arguments;
slot : '$' NUM;
arg : '!' NUM;
capt : '#' NUM;

targ : type_info;

entity_name : package_name '::' id;
variance: (CO | CONTRA)?;
variant_type_info : variance WS* type_info;
type_info : root_type | subst_type | templ_type;
root_type : '{' WS* cat=NUM WS* ',' WS* start=NUM WS* ',' WS* end=NUM WS* '}';
subst_type : '?' NUM;
templ_type : root_type WS* '[' (WS* variant_type_info WS* (',' WS* variant_type_info)*)? WS* ']';
package_name : id ('.' id)*;
id : TICK? v1=ID;

TICK: '`';
SIG: '<sig>';
LOCALS: '<locals>';
RECURSION: '<tail-recursive>';
CTX_RECURSION: '<tail-ctx-recursive>';
BODY: '<body>';
LET: '<let>';
IF: '<if>';
THEN: '<then>';
ELSE: '<else>';
SUB_TYPE_OF: '<subTypeOf>';
FIELD : '<field>';
TYPE_OF : '<typeOf>';
CREATE : '<create>';
TAILCREATE : '<tail-create>';
MULTI_METHOD : '<multi_method>';
METHOD : '<method>';
CLOSURE : '<closure>';
CALL : '<call>';
TAILREC : '<tail-rec>';
PRIMITIVE : '<primitive>';
INTNOTE : '<int>';
NUMNOTE : '<num>';
BYTENOTE : '<byte>';
BOOLEAN : '<true>' | '<false>';
CO: '+';
CONTRA: '-';
STRING: '"' ( ESC_SEQ | ~["\\] )* '"' ;
ID : ALPHA (ALPHA|DIGIT)*;
NUM : SIGN? INT;
WS: [ \n\t\r];
NL: [\n];

fragment ALPHA: [_a-zA-Z];
fragment DIGIT: [0-9] ;
fragment SIGN : '+' | '-';
fragment INT : DIGIT+ ;
fragment ESC_SEQ : '\\' [btnfr"'\\];
