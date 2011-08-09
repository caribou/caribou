triface.admin = function() {
    var template = function() {
        var mtable = " \
<table cellspacing=5> \
  <thead> \
    <tr> \
    {{#headings}} \
      <th>{{.}}</th>\
    {{/headings}} \
    </tr> \
  </thead> \
  <tbody> \
  {{#items}} \
    <tr> \
    {{#headings}} \
      <td>{{item[heading]}}</td> \
    {{/headings}} \
    </tr> \
  {{/items}} \
  </tbody> \
</table>";

        var column = _.template('<td><%= value %></td>');
        var heading = _.template('<th><%= value %></th>');
        var row = _.template('<tr><%= _.map(fields, function(f) {return cell({value: f})}).join("") %></tr>');
        var table = _.template('<table cellspacing=5><thead><%= row({fields: fields, cell: heading}) %></thead><tbody><%= _.map(rows, function(model) {return row({fields: _.map(fields, function(f) {return model[f];}), cell: column})}).join("") %></tbody></table>');

        return {
            table: function(rows) {
                return table({
                    rows: rows,
                    fields: _.keys(_.first(rows)),
                    row: row,
                    heading: heading,
                    column: column
                });
            }
        };
    }();

    return {
        init: function() {
            triface.api.get({
                url: '/model',
                success: function(response) {
                    var body = template.table(response);
                    $('#triface').append(body);
                    console.log(response);
                }
            });
        }
    };
}();