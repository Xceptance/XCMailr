/**
 * Prevents the auto-selection of the first element in Bootstraps Typeahead
 * found at: http://bibwild.wordpress.com/2013/04/04/overriding-bootstrap-typeahead-to-not-initially-select/
 * by Jonathan Rochkind 
 */

var newRender = function(items) {
     var that = this;
 
     items = $(items).map(function (i, item) {
       i = $(that.options.item).attr('data-value', item);
       i.find('a').html(that.highlighter(item));
       return i[0];
     });
 
     this.$menu.html(items);
     return this;
};
$.fn.typeahead.Constructor.prototype.render = newRender;

$.fn.typeahead.Constructor.prototype.select = function() {
    var val = this.$menu.find('.active').attr('data-value');
    if (val) {
      this.$element
        .val(this.updater(val))
        .change();
    }
    return this.hide();
};
