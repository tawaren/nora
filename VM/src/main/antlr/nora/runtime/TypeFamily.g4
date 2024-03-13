grammar TypeFamily;

@header {
    package nora.runtime;
}

family_def: WS*
    FAMILY ':' WS* family=NUM WS+
    LENGTH ':' WS* length=NUM WS+
    DEFS ':' WS* defs=names WS+
    STRUCTURE ':' WS* structure WS*
    EOF;
structure : NUM (WS+ NUM)*;
names : entity_name (WS+ entity_name)*;
entity_name : package_name '::' id;
package_name : id ('.' id)*;
id : v1=ID;

ID : ALPHA (ALPHA|DIGIT)*;
FAMILY : '<family>';
LENGTH : '<length>';
STRUCTURE: '<structure>';
DEFS: '<definitions>';
NUM : SIGN? [0-9]+;
WS: [ \n\t\r];
NL: [\n];

fragment ALPHA: [_a-zA-Z];
fragment DIGIT: [0-9] ;
fragment SIGN : '+' | '-';
