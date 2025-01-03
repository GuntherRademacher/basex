(:~
 : Query resources.
 :
 : @author Christian Grün, BaseX Team, BSD License
 :)
module namespace dba = 'dba/databases';

import module namespace utils = 'dba/utils' at '../../lib/utils.xqm';

(:~
 : Runs a query on a document and returns the result as string.
 : @param  $name      name of database
 : @param  $resource  resource
 : @param  $query     query
 : @return result string
 :)
declare
  %rest:POST('{$query}')
  %rest:path('/dba/db-query')
  %rest:query-param('name',     '{$name}')
  %rest:query-param('resource', '{$resource}')
  %rest:single
  %output:method('text')
function dba:db-query(
  $name      as xs:string,
  $resource  as xs:string,
  $query     as xs:string?
) as xs:string {
  utils:query(
    $query[.] otherwise '.',
    let $type := db:type($name, $resource)
    return head(if($type = 'xml') then (
      db:get($name, $resource)
    ) else if($type = 'binary') then (
      db:get-binary($name, $resource)
    ) else (
      db:get-value($name, $resource)
    ))
  )
};
