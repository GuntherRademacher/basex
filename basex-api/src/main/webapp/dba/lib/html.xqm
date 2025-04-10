(:~
 : HTML components.
 :
 : @author Christian Grün, BaseX Team, BSD License
 :)
module namespace html = 'dba/html';

import module namespace config = 'dba/config' at 'config.xqm';
import module namespace utils = 'dba/utils' at 'utils.xqm';

(: Number formats. :)
declare variable $html:NUMBER := ('decimal', 'number', 'bytes');

(:~
 : Extends the specified table rows with the page template.
 : The following options can be specified:
 : * header: page headers
 : * error: error string
 : * info: info string
 :
 : @param  $rows     table rows
 : @param  $options  options
 : @return page
 :)
declare function html:wrap(
  $rows     as element(tr)*,
  $options  as map(*) := {}
) as element(html) {
  let $header := head($options?header) ! utils:capitalize(.)
  let $user := session:get($config:SESSION-KEY)
  return <html>
    <head>
      <meta charset='utf-8'/>
      <title>DBA{ ($header, tail($options?header)) ! (' » ' || .) }</title>
      <meta name='description' content='Database Administration'/>
      <meta name='author' content='BaseX Team, BSD License'/>
      <meta name="robots" content="noindex"/>
      <link rel='icon' href='static/basex.svg'/>
      <link rel='stylesheet' type='text/css' href='static/style.css'/>
      <link rel='stylesheet' type='text/css' href='static/codemirror/codemirror.css'/>
      <script src='static/js.js'/>
      <script src='static/editor.js'/>
      <script src='static/codemirror/codemirror.js'/>
      <script src='static/codemirror/xquery.js'/>
      <script src='static/codemirror/xml.js'/>
    </head>
    <body>
      <table cellpadding='0' cellspacing='0'>
        <tr>
          <td class='slick'>
            <table width='100%' cellpadding='0' cellspacing='0'>
              <tr>
                <td>{
                  <span style='float:left'>
                    <h1>BaseX Database Administration</h1>
                  </span>,
                  if ($user) {
                    <span style='float:right'>
                      <b>{ $user }</b> · <a href='logout'>logout</a>
                    </span>
                  }
                }</td>
              </tr>
              <tr>
                <td>
                  <div class='ellipsis'>{
                    if ($user) {
                      let $cats := (
                        for $cat in ('Logs', 'Databases', 'Editor', 'Files', 'Jobs',
                          'Users', 'Sessions', 'Settings')
                        let $link := <a href='{ lower-case($cat) }'>{ $cat }</a>
                        return if ($link = $header) then <b>{ $link }</b> else $link
                      )
                      return (
                        head($cats),
                        tail($cats) ! (' · ', .),
                        (1 to 3) ! '&#x2000;'
                      )
                    } else {
                      <div class='note'>
                        Please enter your admin credentials:
                      </div>
                    },
                    <span>{
                      element b {
                        attribute id { 'info' },
                        let $error := $options?error[.], $info := $options?info[.]
                        return if ($error) {
                          attribute class { 'error' }, $error
                        } else if ($info) {
                          attribute class { 'info' }, $info
                        }
                      }
                    }</span>
                  }</div>
                  <hr/>
                </td>
              </tr>
            </table>
          </td>
          <td class='slick'>
            <a href='/'><img src='static/basex.svg'/></a>
          </td>
        </tr>
      </table>
      <table width='100%'>{ $rows }</table>
      <hr/>
      <div id='footer' class='right'><sup>BaseX Team, BSD License</sup></div>
      <div class='small'/>
      { html:js('buttons();') }
    </body>
  </html>
};

(:~
 : Creates an option checkbox.
 : @param  $value  value
 : @param  $label  label
 : @param  $opts   checked options
 : @return checkbox
 :)
declare function html:option(
  $value  as xs:string,
  $label  as xs:string,
  $opts   as xs:string*
) as node()+ {
  html:checkbox('opts', $value, $opts = $value, $label)
};

(:~
 : Creates a checkbox.
 : @param  $name     name of checkbox
 : @param  $value    value
 : @param  $checked  checked state
 : @param  $label    label
 : @return checkbox
 :)
declare function html:checkbox(
  $name     as xs:string,
  $value    as xs:string,
  $checked  as xs:boolean,
  $label    as xs:string
) as node()+ {
  html:checkbox($label, map:merge((
    { 'name':  $name },
    { 'value': $value },
    { 'checked': $checked }[$checked]
  )))
};

(:~
 : Creates a checkbox.
 : @param  $label  label of checkbox
 : @param  $map    additional attributes
 : @return checkbox
 :)
declare function html:checkbox(
  $label   as xs:string,
  $map     as map(*)
) as node()+ {
  element input {
    attribute type { 'checkbox' },
    map:for-each($map, fn($key, $value) { attribute { $key } { $value } })
  },
  text { $label },
  element br { }
};

(:~
 : Creates a button.
 : @param  $action   button action
 : @param  $label    label
 : @param  $options  options: 'CONFIRM' (show confirmation dialog), 'CHECK' (consider checkboxes)
 : @return button
 :)
declare function html:button(
  $action   as xs:string,
  $label    as xs:string,
  $options  as enum('CONFIRM', 'CHECK')* := ()
) as element(button) {
  <button>{
    attribute formaction { $action }[$action],
    attribute onclick { 'return confirm("Are you sure?");' }[$options = 'CONFIRM'],
    attribute data-check { 'check' }[$options = 'CHECK'],
    $label
  }</button>
};

(:~
 : Creates a property list.
 : @param  $props  properties
 : @return table
 :)
declare function html:properties(
  $props  as element()
) as element(table) {
  <table>{
    for $header in $props/*
    return (
      <tr>
        <th colspan='2' align='left'>
          <h3>{ upper-case(name($header)) }</h3>
        </th>
      </tr>,
      for $option in $header/*
      let $value := $option/data()
      return <tr>
        <td><b>{ upper-case($option/name()) }</b></td>
        <td>{
          '✓'[$value = 'true'] otherwise '–'[$value = 'false'] otherwise $value
        }</td>
      </tr>
    )
  }</table>
};

(:~
 : Creates a table for the specified entries.
 : * The table format is specified by the table headers:
 :   * The element names serve as column keys.
 :   * The string values are the header labels.
 :   * The 'type' attribute defines how the values are formatted and sorted:
 :     * 'number': sorted as numbers
 :     * 'decimal': sorted as numbers, output with two decimal digits
 :     * 'bytes': sorted as numbers, output in a human-readable format
 :     * dateTime', 'time': sorted and output as dates
 :     * 'dynamic': function generating dynamic input; sorted as strings
 :     * 'id': suppressed (only used for creating checkboxes)
 :     * otherwise, sorted and output as strings
 :   * The 'order' attribute defines how sorted values will be ordered:
 :     * 'desc': descending order
 :     * otherwise, ascending order
 :   * The 'main' attribute indicates which column is the main column
 : * The supplied table rows are supplied as elements. Values are contained in attributes; their
 :   names represents the column key.
 : * Supplied buttons will placed on top of the table.
 : * Query parameters will be included in table links.
 : * The options argument can have the following keys:
 :   * 'sort': key of the ordered column; if empty, sorting will be disabled
 :   * 'presort': key of pre-sorted column; if identical to sort, entries will not be resorted
 :   * 'link': function for generating a link reference
 :   * 'page': currently displayed page
 :   * 'count': maximum number of results
 :
 : @param  $headers  table headers
 : @param  $entries  table entries
 : @param  $buttons  buttons
 : @param  $params   additional query parameters
 : @param  $options  additional options
 : @return table
 :)
declare function html:table(
  $headers  as map(*)*,
  $entries  as map(*)*,
  $buttons  as element(button)* := (),
  $params   as map(*) := {},
  $options  as map(*) := {}
) as element()+ {
  (: display buttons :)
  if ($buttons) {
    $buttons ! (., <span> </span>),
    <br/>,
    <div class='small'/>
  },

  (: sort entries :)
  let $sort := $options?sort
  let $sorted-entries := (
    let $key := head(($sort[.], head($headers)?key))
    return if (not($sort) or $key = $options?presort) {
      $entries
    } else {
      let $header := $headers[?key = $key]
      let $value := (
        let $desc := $header?order = 'desc'
        return switch($header?type) {
          case 'decimal' case 'number' case 'bytes' return
            if ($desc) {
              fn { 0 - number() }
            } else {
              fn { number() }
            }
          case 'time' case 'dateTime' return
            if ($desc) {
              fn { xs:dateTime('0001-01-01T00:00:00Z') - xs:dateTime(.) }
            } else {
              identity(?)
            }
          case 'dynamic' return
            fn { if (. instance of fn(*)) then string-join(.()) else . }
          default return
            identity(?)
        }
      )
      for $entry in $entries
      order by $value($entry($key)) empty greatest collation '?lang=en'
      return $entry
    }
  )

  (: show results :)
  let $max-option := config:get($config:MAXROWS)
  let $count-option := $options?count[not($sort)]
  let $page-option := $options?page

  let $entries := $count-option otherwise count($sorted-entries)
  let $last-page := ($entries - 1) idiv $max-option + 1
  let $curr-page := min((max(($page-option, 1)), $last-page))
  return (
    (: result summary :)
    element h3 {
      $entries,
      if ($entries = 1) then ' Entry' else 'Entries',

      if ($page-option and $last-page != 1) {
        '(Page: ',
        let $pages := sort(distinct-values((
          1,
          $curr-page - $last-page idiv 10,
          $curr-page - 1,
          $curr-page,
          $curr-page + 1,
          $curr-page + $last-page idiv 10,
          $last-page
        ))[. >= 1 and . <= $last-page])
        for $page at $pos in $pages
        let $suffix := (if ($page = $last-page) then ')' else ' ') ||
          (if ($pages[$pos + 1] > $page + 1) then ' … ' else ())
        return if ($curr-page = $page) {
          $page || $suffix
        } else {
          html:link(string($page), '', ($params, { 'page': $page, 'sort': $sort })),
          $suffix
        }
      }
    },

    (: list of results :)
    let $shown-entries := if ($count-option) {
      $sorted-entries
    } else {
      let $first := ($curr-page - 1) * $max-option + 1
      return $sorted-entries[position() >= $first][position() <= $max-option + 1]
    }
    where exists($shown-entries)
    return element table {
      element tr {
        for $header at $pos in $headers
        let $name := $header?key
        let $label := upper-case($header?label)
        return element th {
          attribute align {
            if ($header?type = $html:NUMBER) then 'right' else 'left'
          },

          if ($pos = 1 and $buttons) {
            <input type='checkbox' onclick='toggle(this)'/>, ' '
          },

          if ($header?type = 'id') {
            (: id columns: empty header column :)
          } else if (empty($sort) or $name = $sort) {
            (: sorted column, xml column: only display label :)
            $label
          } else {
            (: generate sort link :)
            html:link($label, '', ($params, { 'sort': $name }))
          }
        }
      },

      let $link := $options?link
      for $entry in $shown-entries[position() <= $max-option]
      return element tr {
        $entry?id ! attribute id { . },
        for $header at $pos in $headers
        let $name := $header?key
        let $type := $header?type

        (: format value :)
        let $v := $entry($name)
        let $value := try {
          if ($type = 'bytes') {
            prof:human(if (exists($v)) then xs:integer($v) else 0)
          } else if ($type = 'decimal') {
            format-number(if (exists($v)) then number($v) else 0, '0.00')
          } else if ($type = 'dateTime') {
            html:date(xs:dateTime($v))
          } else if ($type = 'time') {
            html:time(xs:dateTime($v))
          } else if ($v instance of fn(*)) {
            $v()
          } else {
            string($v)
          }
        } catch * {
          $err:description
        }
        return element td {
          attribute align { if ($type = $html:NUMBER) then 'right' else 'left' },
          if ($pos = 1 and $buttons) {
            <input type='checkbox' name='{ $name }' value='{ data($value) }'
              onclick='buttons(this)'/>,
            ' '
          },
          if ($pos = 1 and exists($link)) {
            html:link($value, $link, ($params, { $name: $value }))
          } else if (not($type = 'id')) {
            $value
          }
        }
      }
    }
  )
};

(:~
 : Creates a link to the specified target.
 : @param  $text    link text
 : @param  $href    link reference
 : @param  $params  query parameters
 : @return link
 :)
declare function html:link(
  $text    as xs:string,
  $href    as xs:string,
  $params  as map(*)* := {}
) as element(a) {
  <a href='{ web:create-url($href, map:merge($params)) }'>{ $text }</a>
};

(:~
 : Returns a formatted representation of a dateTime value.
 : @param  $date  date
 : @return string
 :)
declare function html:date(
  $date  as xs:dateTime
) as xs:string {
  format-dateTime(html:adjust($date), '[Y0000]-[M00]-[D00], [H00]:[m00]:[s00]')
};

(:~
 : Returns a formatted time representation of a dateTime value with tooltip.
 : @param  $date  date
 : @return element with tooltip
 :)
declare function html:time(
  $date  as xs:dateTime
) as element(span) {
  let $adjusted := html:adjust($date)
  let $formatted := format-dateTime(html:adjust($date), '[H00]:[m00]:[s00]')
  return <span title='{ $adjusted }'>{ $formatted }</span>
};

(:~
 : Returns a dateTime value adjusted to the current time zone.
 : @param  $date  date
 : @return adjusted value
 :)
declare function html:adjust(
  $date  as xs:dateTime
) as xs:dateTime {
  let $zone := timezone-from-dateTime(current-dateTime())
  return fn:adjust-dateTime-to-timezone(xs:dateTime($date), $zone)
};

(:~
 : Formats a duration.
 : @param  $seconds  seconds
 : @return string
 :)
declare function html:duration(
  $seconds  as xs:decimal
) as xs:string {
  let $min := $seconds idiv 60
  let $sec := $seconds - $min * 60
  return (format-number($min, '00') || ':' || format-number($sec, '00'))
};

(:~
 : Creates an embedded JavaScript snippet.
 : @param  $js  JavaScript string
 : @return script element
 :)
declare function html:js(
  $js  as xs:string
) as element(script) {
  <script>{ '(function() { ' || $js || ' })();' }</script>
};

(:~
 : Creates a new map with the current query parameters.
 : @return map with query parameters
 :)
declare function html:parameters() as map(*) {
  map:merge(
    for $param in request:parameter-names()[not(starts-with(., '_'))]
    return { $param: request:parameter($param) }
  )
};

(:~
 : Creates a new map with query parameters. The returned map contains all
 : current query parameters, : and the given ones, prefixed with an underscore.
 : @param  $map  predefined parameters
 : @return map with query parameters
 :)
declare function html:parameters(
  $map  as map(*)?
) as map(*) {
  map:merge((
    html:parameters(),
    map:for-each($map, fn($name, $value) {
      map:entry('_' || $name, $value)
    })
  ))
};

(:~
 : Performs an update or returns an HTML output.
 : @param  $do       perform update
 : @param  $options  output options
 : @param  $output   output function
 : @param  $update   update function
 : @return parse result
 :)
declare %updating function html:update(
  $do       as xs:string?,
  $options  as map(*),
  $output   as function() as element(tr)*,
  $update   as %updating function(*)
) {
  if ($do) {
    try {
      updating $update()
    } catch * {
      update:output(html:wrap($output(), map:put($options, 'error', $err:description)))
    }
  } else {
    update:output(html:wrap($output(), $options))
  }
};
