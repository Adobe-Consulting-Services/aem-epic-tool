# aem-epic-tool
External Package Inspect and Comparison Tool for AEM

## What is this?
This is a stand-alone desktop application that allows you to connect to a remote server and perform read-only operations on packages such as:
- Download packages for offline use or for further inspection
- Summarize what is in each package (per base folder or per type)
- Compare versions of a package to see what has changed
- Compare several packages against each other to see which packages overlap each other
- Many table views also support XLSX export for producing documentation

(Note: Java 8 is required to run this tool)

## What versions of AEM is this compatible with?
Pretty much any version of AEM will work with this tool unless there is a breaking change on the package manager REST service, or unless the location of packages changes in the future.  At the present, it has been confirmed to work with stuff as early as 5.6 and as recent as 6.3

## What if it is missing this one cool thing I really want?
Please add an issue in GitHub and we'll take a swing at it as time permits!

## How do I build it?
This is a standard Apache Maven project, so "mvn install" should be sufficient.  If your IDE understands Maven you can use that too.
