grammar Data;

@header {
    package nora.runtime;
}

data_def: WS*
    NAME ':' WS* name=entity_name WS+
    (HANDLER ':' WS* meta=handler_ref WS+)?
    (SUPER ':' WS* super=entity_name WS+)?
    KIND ':' WS* (ABSTRACT | CONCRETE) WS+
    INFO ':' WS* root_type WS+
    GENERICS ':' WS* generics? WS+
    FIELDS ':' WS* (field WS+)* WS*
    EOF;

generics: (full_variance WS* (',' WS* full_variance)*);
variance: (CO | CONTRA)?;
full_variance : INV | CO | CONTRA;
handler_ref: meta=id '#' object=id '(' WS* (handler_arg WS* (',' WS* handler_arg)*)? WS*  ')';
handler_arg : NUM | entity_name | STRING | type_info | BOOL;
field: id WS* ':' WS* type_info;
entity_name : package_name '::' id;
package_name : id ('.' id)*;
variant_type_info : variance WS* type_info;
type_info : root_type | subst_type | templ_type;
root_type : '{' WS* cat=NUM WS* ',' WS* start=NUM WS* ',' WS* end=NUM WS* '}';
subst_type : '?' NUM;
templ_type : root_type  WS* '[' WS* (variant_type_info WS* (',' WS* variant_type_info)*)? WS* ']';
id : ID;


NAME : '<name>';
KIND : '<kind>';
INFO: '<info>';
SUPER: '<super>';
HANDLER: '<handler>';
PARAMS: '<parameter>';
GENERICS: '<generics>';
FIELDS: '<fields>';
ABSTRACT: '<abstract>';
CONCRETE: '<concrete>';
INV: '~';
CO: '+';
CONTRA: '-';
ID : ALPHA (ALPHA|DIGIT)*;
NUM : SIGN? INT;
BOOL: '<true>' | '<false>';
STRING: '"' ( ESC_SEQ | ~["\\] )* '"' ;
WS: [ \n\t\r];
NL: [\n];

fragment ALPHA: [_a-zA-Z];
fragment DIGIT: [0-9] ;
fragment SIGN : '+' | '-';
fragment INT : DIGIT+ ;
fragment ESC_SEQ : '\\' [btnfr"'\\];
