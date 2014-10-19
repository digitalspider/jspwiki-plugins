
function applyPanelJS() {
  var panels = document.getElementsByClassName('panel');
  for (i = 0; i < panels.length; i++) {
    var panel = panels[i];
    var panelId = panel.id;
    if (panelId) {
      var headerId = panelId.replace("panel","header");
      var footerId = panelId.replace("panel","footer");
      var contentId = panelId.replace("panel","content");
      headerEle = document.getElementById(headerId);
      footerEle = document.getElementById(footerId);
      contentEle = document.getElementById(contentId);

      // Width
      var width = panel.dataset.width;
      if (width) {
        panel.style.width=width;
      }
      var width = panel.dataset.minwidth;
      if (width) {
        panel.style.minWidth=width;
      }
      // Height
      var height = panel.dataset.height;
      if (height) {
        panel.style.height=height;
      }
      var height = panel.dataset.minheight;
      if (height) {
        panel.style.minHeight=height;
      }
      // Color background
      var color = panel.dataset.colorpanelbg;
      if (color && contentEle) {
        panel.style.backgroundColor=color;
      }
      var color = panel.dataset.colorcontentbg;
      if (color && contentEle) {
        contentEle.style.backgroundColor=color;
      }
      var color = panel.dataset.colorheaderbg;
      if (color && headerEle) {
        headerEle.style.backgroundColor=color;
      }
      var color = panel.dataset.colorfooterbg;
      if (color && footerEle) {
        footerEle.style.backgroundColor=color;
      }
      // Color text
      var color = panel.dataset.colorpaneltext;
      if (color && contentEle) {
        panel.style.color=color;
      }
      var color = panel.dataset.colorcontenttext;
      if (color && contentEle) {
        contentEle.style.color=color;
      }
      var color = panel.dataset.colorheadertext;
      if (color && headerEle) {
        headerEle.style.color=color;
      }
      var color = panel.dataset.colorfootertext;
      if (color && footerEle) {
        footerEle.style.color=color;
      }
      // Border
      var border = panel.dataset.border;
      if (border) {
        panel.style.border=border;
      }
      // Color border
      var color = panel.dataset.colorborder;
      if (color) {
        panel.style.borderColor=color;
      }
      // Margin
      var margin = panel.dataset.margin;
      if (margin) {
        panel.style.margin=margin;
      }
      // Padding
      var padding = panel.dataset.padding;
      if (padding) {
        if(headerEle) {
          headerEle.style.padding=padding;
        }
        if(contentEle) {
          contentEle.style.padding=padding;
        }
        if(footerEle) {
          footerEle.style.padding=padding;
        }
      }
      // Rounded Corners
      var corners = panel.dataset.corners;
      if (corners) {
        panel.style.borderRadius=corners;
        if (headerEle) {
          headerEle.style.borderTopLeftRadius=corners;
          headerEle.style.borderTopRightRadius=corners;      
        }
        if (footerEle) {
          footerEle.style.borderBottomLeftRadius=corners;
          footerEle.style.borderBottomRightRadius=corners;      
        }
        if (!headerEle && contentEle) {
          contentEle.style.borderTopLeftRadius=corners;
          contentEle.style.borderTopRightRadius=corners;
        }
        if (!footerEle && contentEle) {
          contentEle.style.borderBottomLeftRadius=corners;
          contentEle.style.borderBottomRightRadius=corners;
        }
      }
    }
  }
}

document.addEventListener('DOMContentLoaded', applyPanelJS, false);
