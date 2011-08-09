triface.admin = function() {
    var template = function() {
        var column = _.template('<td><%= m[f] %></td>');
        var heading = _.template('<th><%= h %></th>');
        var row = _.template('<tr><%= fields.join("") %></tr>');
        var table = _.template('<table cellspacing=5><thead><%= row({fields: _.map(fields, function(f) {return heading({h: f})})}) %></thead><tbody><%= _.map(rows, function(model) {return row({fields: _.map(fields, function(f) {return column({m: model, f: f})})});}).join("") %></tbody></table>');

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