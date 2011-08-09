triface.admin = function() {
    return {
        init: function() {
            triface.api.get({
                url: '/model',
                success: function(response) {
                    var directives = {
                        'li': {
                            'model<-models':{'.':'model.name'}
                        },
                    };
                    var models = {'headers': ['name', 'id'], 'models': response};
                    var body = $('<ul><li class="models"></li></ul>').directives(directives).render(models);
                    $('#triface').append(body);
                    console.log(response);

                    // var directives = {
                    //     'th': {
                    //         'header<-headers':{'.':'header'}
                    //     },
                    //     'td': {
                    //         'model<-models':{'.':'model'}
                    //     }
                    // };

                    // var table = '<table class="models"><thead><tr><th class="headings"></th></tr></thead><tbody><tr><td></td></tr></tbody></table>';
                    //var body = $(table).directives(directives).render(models);
                }
            });
        }
    };
}();