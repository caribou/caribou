var triface = function() {
    return {
        init: function() {
            $.ajax({
                url: 'http://api.triface.local/model',
                complete: function(result) {
                    console.log(result);
                }
            });
        }
    };
}();