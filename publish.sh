#!/bin/bash

DSDIR=/opt/digitalspider
PLUGINSDIR=$DSDIR/jspwiki-plugins
WIKIDIR=$CATALINA_HOME/webapps/JSPWiki
WEBLIBDIR=$WIKIDIR/WEB-INF/lib

cp $PLUGINSDIR/MyPlugin/dist/MyPlugin-0.1.jar $WEBLIBDIR/
cp $PLUGINSDIR/TreePlugin/dist/TreePlugin-0.9.jar $WEBLIBDIR/
cp $PLUGINSDIR/HitCountPlugin/dist/HitCountPlugin-0.1.jar $WEBLIBDIR/
cp $PLUGINSDIR/GoComicsPlugin/dist/GoComicsPlugin-0.1.jar $WEBLIBDIR/
cp $PLUGINSDIR/ImageGalleryPlugin/dist/ImageGalleryPlugin-0.1.jar $WEBLIBDIR/
cp $PLUGINSDIR/VideoPlugin/dist/VideoPlugin-0.1.jar $WEBLIBDIR/
cp $PLUGINSDIR/JiraPlugin/dist/JiraPlugin-0.1.jar $WEBLIBDIR/
unzip -q -o $PLUGINSDIR/ImageGalleryPlugin/dist/jssor-templates.zip -d $WIKIDIR/

