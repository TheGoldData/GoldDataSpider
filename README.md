GoldDataSpider
===============

Introduction
---

The Gold Data Spider is used for fetching pages and extracting data


An API for fetching and extracting data from web pages. It combines fetching pages and extracting data features together.


It defines an simple and agile structure or a rule syntax, used for extracting data in scripts embed in the JDK . The structure can also update easily as the web pages' changes.

It can support  the various types of documents for extracting data  from web pages, for example: html/xml/json/javascript,etc.

Getting Started
---

First we add the jar to the project classpath.  

1. maven:
```maven
<dependency>
  <groupId>com.100shouhou.golddata</groupId>
  <artifactId>golddata-spider</artifactId>
  <version>1.1.1</version>
</dependency>
```
2、gradle：
```gradle
 compile group: 'com.100shouhou.golddata', name: 'golddata-spider', version: '1.1.1'
```

Then you can use the spider,for example:

```java
@Test
public void testGoldSpider(){
    String ruleContent=
            "    {                                                      \n"+
            "      __node: li.sky.skyid                                 \n"+
            "      date:                                                \n"+
            "      {                                                    \n"+
            "        expr: h1                                           \n"+
            "        __label: 日期                                      \n"+
            "      }                                                    \n"+
            "      sn:                                                  \n"+
            "      {                                                    \n"+
            "                                                           \n"+
            "        js: md5(baseUri+item.date+headers['Content-Type']);\n"+
            "      }                                                    \n"+
            "      weather:                                             \n"+
            "      {                                                    \n"+
            "        expr: p.wea                                        \n"+
            "      }                                                    \n"+
            "      temprature:                                          \n"+
            "      {                                                    \n"+
            "        expr: p.tem>i                                      \n"+
            "      }                                                    \n"+
            "    }                                                      \n";
    GoldSpider spider= com.xst.golddata.GoldSpider.newSpider()
            .setUrl("http://www.weather.com.cn/weather/101020100.shtml")
            .setRule(ruleContent)
            .request();
    List list=spider.extractList();
    // List<Weather> weathers=spider.extractList(Weather.class);
    // Weather weathers=spider.extractFirst(Weather.class);
   list.forEach( System.out::println);
}
``` 

Running the test, you will see the output just like as follow:
```shell
{date=19日（今天）, weather=阴转小雨, temprature=10℃, sn=8bc265cb2bf23b6764b75144b255d81d}
{date=20日（明天）, weather=小雨转多云, temprature=11℃, sn=9efd7e7bbbfb9bb06e04c0c990568bfd}
{date=21日（后天）, weather=多云转中雨, temprature=11℃, sn=728539ac882721187741708860324afa}
{date=22日（周六）, weather=小雨, temprature=9℃, sn=a23fa2233e750a3bdd11b2e200ed06c3}
{date=23日（周日）, weather=小雨转多云, temprature=8℃, sn=b27e1b8a8e92a7bed384ceb3e4fdfb5f}
{date=24日（周一）, weather=多云转小雨, temprature=8℃, sn=c142b7fd12330ca031dd96b307c0d50d}
{date=25日（周二）, weather=小雨转中雨, temprature=6℃, sn=16f71d3c8f09394588532a3ed1a8bacf}
```

In an addition,You can free edit the rule content using the visual rule editor from the golddata platform. It can be downloaded at [GoldData Official Website](https://golddata.100shouhou.com/front/download) . The visual rule editor screenshot is as follow:

![Visual Rule Editor](images/visualEditor.jpg)


Documentation
---

Please see the documents at [https://golddata.100shouhou.com](https://golddata.100shouhou.com/front/docs)



License
---

Golddata-Spider is licensed under the terms of the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0.html).
