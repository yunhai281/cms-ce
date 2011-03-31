(function(){tinymce.PluginManager.requireLangPack('cmsimage');var reCMSImagePattern=/_image\/(\d+)/im;tinymce.create('tinymce.plugins.CMSImagePlugin',{init:function(ed,url){var t=this;ed.addCommand('cmsimage',function(){var pageToOpen='adminpage?page=600&op=popup&subop=insert'+'&selectedunitkey=-1'+'&fieldname=null'+'&fieldrow=null'+'&handler=com.enonic.vertical.adminweb.handlers.ContentEnhancedImageHandlerServlet'+'&contenthandler=image';var dom=ed.dom;var selection=ed.selection;var selectedNode=selection.getNode();var imageContentKey;if(selectedNode.nodeName==='IMG'){if(dom.getAttrib(selectedNode,'src').match(reCMSImagePattern)){imageContentKey=dom.getAttrib(selectedNode,'src').match(reCMSImagePattern)[1];pageToOpen='adminpage?page=992&key='+imageContentKey+'&cat=-1&op=insert&subop=update'}}ed.windowManager.open({file:pageToOpen,width:990+ed.getLang('cmsimage.delta_width',0),height:620+ed.getLang('cmsimage.delta_height',0),inline:1,scrollbars:'yes'},{plugin_url:url})});ed.addButton('cmsimage',{title:'cmsimage.desc',cmd:'cmsimage',image:url+'/img/image.gif'});ed.onKeyUp.add(function(ed,e){if(e.keyCode===13){var dom=ed.dom;var selection=ed.selection;var currentNode=selection.getNode();var currentNodeHasCMSImageClass=dom.hasClass(currentNode,'editor-p-block')||dom.hasClass(currentNode,'editor-p-center');if(currentNodeHasCMSImageClass){dom.removeClass(currentNode,'editor-p-block');dom.removeClass(currentNode,'editor-p-center')}}});ed.onNodeChange.add(function(ed,cm,n){var oDOM=ed.dom;var oParentNode=n.parentNode;var bSetCMSImageButtonActive=n.nodeName=='IMG'&&oDOM.getAttrib(n,'src').match(reCMSImagePattern)&&n.className.indexOf('mceItem')==-1;var bSetJustifyLeftButtonActive=n.nodeName=='IMG'&&oDOM.hasClass(n,'editor-image-left')||oDOM.getStyle(n,'textAlign')=='left';var bSetJustifyRightButtonActive=n.nodeName=='IMG'&&oDOM.hasClass(n,'editor-image-right')||oDOM.getStyle(n,'textAlign')=='right';var bSetJustifyCenterButtonActive=oDOM.hasClass(oParentNode,'editor-p-center')||oDOM.getStyle(n,'textAlign')=='center';var bSetJustifyFullButtonActive=oDOM.hasClass(oParentNode,'editor-p-block')||oDOM.getStyle(n,'textAlign')=='justify';cm.setActive('cmsimage',bSetCMSImageButtonActive);cm.setActive('justifyleft',bSetJustifyLeftButtonActive);cm.setActive('justifyright',bSetJustifyRightButtonActive);cm.setActive('justifycenter',bSetJustifyCenterButtonActive);cm.setActive('justifyfull',bSetJustifyFullButtonActive)});var oSelectedImage=null;ed.onMouseDown.add(function(ed,e){if(!tinymce.isGecko){var oDOM=ed.dom;if(e.target.nodeName==='IMG'){oSelectedImage=e.target;var parent=e.target.parentNode;if(parent.nodeName==='P'&&(oDOM.hasClass(parent,'editor-p-block')||oDOM.hasClass(parent,'editor-p-center'))){ed.selection.select(parent)}}}});ed.onMouseUp.add(function(ed,e){if(!tinymce.isGecko){var oDOM=ed.dom;if(e.target.nodeName==='IMG'){oSelectedImage=e.target;var parent=e.target.parentNode;if(parent.nodeName==='P'&&(oDOM.hasClass(parent,'editor-p-block')||oDOM.hasClass(parent,'editor-p-center'))){ed.selection.select(e.target)}}}});ed.onBeforeExecCommand.add(function(ed,cmd,ui,val){if(cmd==='JustifyLeft'||cmd==='JustifyRight'||cmd==='JustifyCenter'||cmd==='JustifyFull'){var oDOM=ed.dom;if(tinymce.isGecko)oSelectedImage=ed.selection.getNode();if(oSelectedImage&&oSelectedImage.nodeName==='IMG'){t.removeCmsCssFromImage(ed.selection.getNode(),ed);oDOM.setAttrib(oSelectedImage,'cmsimage','1');if(cmd==='JustifyCenter'&&oDOM.hasClass(oSelectedImage.parentNode,'editor-p-block')){oDOM.setAttrib(oSelectedImage.parentNode,'cmsp','1')}}}});ed.onExecCommand.add(function(ed,cmd,ui,val){if(!/Justify(left|right|center|full)/i.test(cmd)){return}var oDOM=ed.dom;var oImageElement=oSelectedImage;if(oImageElement&&oImageElement.nodeName==='IMG'){if(cmd==='JustifyLeft'){t.alignImage(ed,oImageElement,'left')}else if(cmd==='JustifyRight'){t.alignImage(ed,oImageElement,'right')}else if(cmd==='JustifyCenter'){oImageElement=oDOM.select('img[cmsimage=1]')[0];t.centerImage(ed,oImageElement);var oCMSPBlock=oDOM.select('p[cmsp=1]')[0];if(oDOM.hasClass(oCMSPBlock,'editor-p-block')){oDOM.remove(oCMSPBlock)}}else if(cmd==='JustifyFull'){t.blockImage(ed,oImageElement)}ed.execCommand('mceRepaint');oImageElement=oDOM.select('img[cmsimage=1]')[0];oDOM.setAttrib(oImageElement,'cmsimage');ed.selection.select(oImageElement);oDOM.setAttrib(oImageElement,'_moz_resizing','true');oSelectedImage=oImageElement}})},createControl:function(n,cm){return null},getInfo:function(){return{longname:'Image Browser for CMS',author:'Enonic',authorurl:'http://www.enonic.com',infourl:'http://www.enonic.com',version:"0.2"}},alignImage:function(ed,imageElement,align){var t=this;var dom=ed.dom;var parentParagraphToImage=dom.getParent(imageElement,'p');var imageHasLink=imageElement.parentNode.nodeName==='A';var isImageInsideCMSParagraph,closestParagraph,isSiblingToImageParagraph;dom.setStyle(imageElement,'float','');dom.addClass(imageElement,'editor-image-'+align);if(imageHasLink){dom.removeClass(imageElement.parentNode,'editor-image-left');dom.removeClass(imageElement.parentNode,'editor-image-right')}isImageInsideCMSParagraph=parentParagraphToImage&&parentParagraphToImage.nodeName==='P'&&(dom.hasClass(parentParagraphToImage,'editor-p-block')||dom.hasClass(parentParagraphToImage,'editor-p-center'));if(isImageInsideCMSParagraph){dom.remove(parentParagraphToImage,true)}else{return}closestParagraph=(imageHasLink)?t.getNextSiblingElement(imageElement.parentNode):t.getNextSiblingElement(imageElement);isSiblingToImageParagraph=closestParagraph&&closestParagraph.nodeName==='P';if(!isSiblingToImageParagraph){closestParagraph=(imageHasLink)?t.getPrevSiblingElement(imageElement.parentNode):t.getPrevSiblingElement(imageElement)}if(closestParagraph&&closestParagraph.nodeName==='P'){var elementToClone=null;if(imageHasLink){elementToClone=imageElement.parentNode;t.prependChild(closestParagraph,elementToClone.cloneNode(true))}else{elementToClone=imageElement;t.prependChild(closestParagraph,elementToClone.cloneNode(false))}dom.remove(elementToClone,false)}else{var serializer=new tinymce.dom.Serializer();var elementAsString=(imageHasLink)?serializer.serialize(imageElement.parentNode):serializer.serialize(imageElement);var elementToRemove=(imageHasLink)?imageElement.parentNode:imageElement;ed.selection.setContent('<p>'+elementAsString+'<br/></p>');dom.remove(elementToRemove,false)}},blockImage:function(ed,imageElement){var dom=ed.dom;var parentParagraphToImage=dom.getParent(imageElement,'p');var imageHasLink=imageElement.parentNode.nodeName==='A';var isImageBlockAligned,isImageCenterAligned;var elementToClone;dom.setStyle(parentParagraphToImage,'textAlign','');isImageBlockAligned=parentParagraphToImage&&parentParagraphToImage.nodeName==='P'&&dom.hasClass(parentParagraphToImage,'editor-p-block');if(isImageBlockAligned){return}isImageCenterAligned=parentParagraphToImage&&parentParagraphToImage.nodeName==='P'&&dom.hasClass(parentParagraphToImage,'editor-p-center');if(isImageCenterAligned){dom.removeClass(parentParagraphToImage,'editor-p-center');dom.addClass(parentParagraphToImage,'editor-p-block');return}var parentElementToImage=dom.getParent(imageElement,dom.isBlock);var newParagraphForImage;if(!parentElementToImage||parentElementToImage.childNodes.length>1){newParagraphForImage=dom.create('p',{'class':'editor-p-block'});if(imageHasLink){elementToClone=imageElement.parentNode;newParagraphForImage.appendChild(elementToClone.cloneNode(true))}else{elementToClone=imageElement;newParagraphForImage.appendChild(elementToClone.cloneNode(false))}parentElementToImage.parentNode.insertBefore(newParagraphForImage,parentElementToImage);dom.remove(elementToClone);imageElement=newParagraphForImage.firstChild;parentElementToImage=newParagraphForImage}},centerImage:function(ed,imageElement){var t=this;var dom=ed.dom;var parentParagraphToImage=dom.getParent(imageElement,'p');var imageHasLink=imageElement.parentNode.nodeName==='A';var isImageBlockAligned,isImageCenterAligned;var elementToClone;dom.setStyle(parentParagraphToImage,'textAlign','');isImageCenterAligned=parentParagraphToImage&&dom.hasClass(parentParagraphToImage,'editor-p-center');if(isImageCenterAligned){return}isImageBlockAligned=parentParagraphToImage&&dom.hasClass(parentParagraphToImage,'editor-p-block');if(isImageBlockAligned){dom.removeClass(parentParagraphToImage,'editor-p-block');dom.addClass(parentParagraphToImage,'editor-p-center');return}dom.removeClass(parentParagraphToImage,'editor-p-block');dom.removeClass(parentParagraphToImage,'editor-p-center');dom.addClass(parentParagraphToImage,'editor-p-center');var previousElementToParagraph=t.getPrevSiblingElement(parentParagraphToImage);if(previousElementToParagraph&&previousElementToParagraph.nodeName==='P'){if(previousElementToParagraph.parentNode){previousElementToParagraph.parentNode.insertBefore(parentParagraphToImage,previousElementToParagraph)}}},getPrevSiblingElement:function(element){var sibling=(element)?element.previousSibling:null;if(sibling){while(sibling&&sibling.nodeType!=1){sibling=sibling.previousSibling}}return sibling},getNextSiblingElement:function(element){var sibling=(element)?element.nextSibling:null;if(sibling){while(sibling&&sibling.nodeType!=1){sibling=sibling.nextSibling}}return sibling},prependChild:function(parentElement,elementToPrepend){parentElement.insertBefore(elementToPrepend,parentElement.firstChild)},removeCmsCssFromImage:function(element,ed){var dom=ed.dom;dom.removeClass(element,'editor-image-left');dom.removeClass(element,'editor-image-right')}});tinymce.PluginManager.add('cmsimage',tinymce.plugins.CMSImagePlugin)})();