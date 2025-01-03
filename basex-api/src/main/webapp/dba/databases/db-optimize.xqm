(:~
 : Optimize databases.
 :
 : @author Christian Grün, BaseX Team, BSD License
 :)
module namespace dba = 'dba/databases';

import module namespace html = 'dba/html' at '../lib/html.xqm';
import module namespace utils = 'dba/utils' at '../lib/utils.xqm';

(:~ Top category :)
declare variable $dba:CAT := 'databases';
(:~ Sub category :)
declare variable $dba:SUB := 'database';

(:~
 : Form for optimizing a database.
 : @param  $name   entered name
 : @param  $all    optimize all
 : @param  $opts   database options
 : @param  $lang   language
 : @param  $error  error string
 : @return page
 :)
declare
  %rest:GET
  %rest:POST
  %rest:path('/dba/db-optimize')
  %rest:query-param('name',  '{$name}')
  %rest:query-param('all',   '{$all}')
  %rest:query-param('opts',  '{$opts}')
  %rest:query-param('lang',  '{$lang}', 'en')
  %rest:query-param('error', '{$error}')
  %output:method('html')
  %output:html-version('5')
function dba:db-optimize(
  $name   as xs:string,
  $all    as xs:string?,
  $opts   as xs:string*,
  $lang   as xs:string?,
  $error  as xs:string?
) as element(html) {
  let $opts := if($opts = 'x') then $opts else db:info($name)//*[text() = 'true']/name()
  let $lang := if($opts = 'x') then $lang else 'en'
  return html:wrap({ 'header': ($dba:CAT, $name), 'error': $error },
    <tr>
      <td>
        <form method='post' autocomplete='off'>
          <h2>{
            html:link('Databases', $dba:CAT), ' » ',
            html:link($name, 'database', { 'name': $name }), ' » ',
            html:button('db-optimize-do', 'Optimize')
          }</h2>
          <!-- dummy value; prevents reset of options if nothing is selected -->
          <input type='hidden' name='opts' value='x'/>
          <input type='hidden' name='name' value='{ $name }'/>
          <table>
            <tr>
              <td colspan='2'>
                { html:checkbox('all', 'all', exists($all), 'Full optimization') }
                <h3>{ html:option('textindex', 'Text Index', $opts) }</h3>
                <h3>{ html:option('attrindex', 'Attribute Index', $opts) }</h3>
                <h3>{ html:option('tokenindex', 'Token Index', $opts) }</h3>
                <h3>{ html:option('ftindex', 'Fulltext Index', $opts) }</h3>
              </td>
            </tr>
            <tr>
              <td colspan='2'>{
                html:option('stemming', 'Stemming', $opts),
                html:option('casesens', 'Case Sensitivity', $opts),
                html:option('diacritics', 'Diacritics', $opts)
              }</td>
            </tr>
            <tr>
              <td>Language:</td>
              <td>
                <input type='text' name='lang' value='{ $lang }' autofocus='autofocus'/>
                <div class='small'/>
              </td>
            </tr>
          </table>
        </form>
      </td>
    </tr>
  )
};

(:~
 : Optimizes a database.
 : @param  $name  database
 : @param  $all   optimize all
 : @param  $opts  database options
 : @param  $lang  language
 : @return redirection
 :)
declare
  %updating
  %rest:POST
  %rest:path('/dba/db-optimize-do')
  %rest:query-param('name', '{$name}')
  %rest:query-param('all',  '{$all}')
  %rest:query-param('opts', '{$opts}')
  %rest:query-param('lang', '{$lang}')
function dba:db-optimize-do(
  $name  as xs:string,
  $all   as xs:string?,
  $opts  as xs:string*,
  $lang  as xs:string?
) as empty-sequence() {
  try {
    db:optimize($name, boolean($all), map:merge((
      ('textindex','attrindex','tokenindex','ftindex','stemming','casesens','diacritics') !
        map:entry(., $opts = .),
      $lang ! map:entry('language', .)
    ))),
    utils:redirect($dba:SUB, { 'name': $name, 'info': 'Database was optimized.' })
  } catch * {
    utils:redirect($dba:SUB, {
      'name': $name, 'opts': $opts, 'lang': $lang, 'error': $err:description
    })
  }
};

(:~
 : Optimizes databases with the current settings.
 : @param  $names  names of databases
 : @return redirection
 :)
declare
  %updating
  %rest:path('/dba/dbs-optimize')
  %rest:query-param('name', '{$names}')
function dba:dbs-optimize(
  $names  as xs:string*
) as empty-sequence() {
  try {
    $names ! db:optimize(.),
    utils:redirect($dba:CAT, { 'info': utils:info($names, 'database', 'optimized') })
  } catch * {
    utils:redirect($dba:CAT, { 'error': $err:description })
  }
};
