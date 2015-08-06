# On-Demand Document module

This OpenMRS module provides on-demand document support for use within the [OpenSHR project](https://github.com/jembi/openshr).

It supports the following:
 * Registers an on-demand CCD document whenever a the cda-handler module discretely processes a document
 * Handles requests for an on-demand document and generates the actual document using the latest discrete information
 
## Building

Build this module using `mvn clean install`
