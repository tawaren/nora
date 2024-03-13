grammar MultiMethod;

@header {
    package nora.runtime;
}

generic_method_def: WS*
    SIGNATURE ':' WS+ typesig WS+
    METHODS ':' WS+ dispatch_info (WS+ dispatch_info)* WS*
    EOF;
dispatch_info: entity_name WS* genargs? WS* directTypeSig WS* case_filters?;
directTypeSig: WS* typeargs WS* '->' WS* ret=type_info;
typesig : typeargs WS* '|' directTypeSig;
typeargs : '(' WS* (type_info WS* (',' WS* type_info)*)? WS* ')';
genargs : '[' WS* (typParams=NUM WS* '|' WS*)? (type_info WS* (',' WS* type_info)*)? WS* ']';
case_filters : '<'  WS* (case_filter WS* (',' WS* case_filter)*)? WS* '>';
case_filter: meta=id '#' object=id '(' WS* (filter_arg WS* (',' WS* filter_arg)*)? WS*  ')';
//Todo: This needs to get better some injection of services stuff
//      That would be greate either way also for the loader thing
filter_arg : NUM | entity_name | STRING | type_info | BOOL;
entity_name : package_name '::' id;
type_info : root_type | subst_type | templ_type;
root_type : '{' WS* cat=NUM WS* ',' WS* start=NUM WS* ',' WS* end=NUM WS* '}';
subst_type : '?' NUM;
templ_type : root_type  WS* '[' WS* (variant_type_info WS* (',' WS* variant_type_info)*)? WS* ']';
variant_type_info: (CO | CONTRA)? WS* type_info;
package_name : id ('.' id)*;
id : TICK? v1=ID;

TICK: '`';
SIGNATURE: '<sig>';
METHODS: '<methods>';

BOOL: '<true>' | '<false>';
STRING: '"' ( ESC_SEQ | ~["\\] )* '"' ;
CO: '+';
CONTRA: '-';
ID : ALPHA (ALPHA|DIGIT)*;
NUM : SIGN? INT;
WS: [ \n\t\r];
NL: [\n];

fragment ALPHA: [_a-zA-Z];
fragment DIGIT: [0-9] ;
fragment SIGN : '+' | '-';
fragment INT : DIGIT+ ;
fragment ESC_SEQ : '\\' [btnfr"'\\];
