## Inspiration
Visualising geology beneath the surface when on site is a tough gig. Where does that rock go once it’s swallowed up by the earth? What if the site has no rocks exposed at surface: how are you meant to visualise what the geology is like without having to go back to the office and check your 3D geological model? How are you meant to explain easily to stakeholders visiting on site that there is a sandstone aquifer in the middle of your site, but that building to the west of it wouldn’t compromise ground water quality? Without bringing them back into the office to view your 3D geological model, it’s very difficult. 

As a geologist in her previous profession, Rachael came across this problem whilst leading groups (stakeholders, engineering geologists) on field work for site investigations. She’d bring paper print outs of 2D maps and the associated 3D geological models: but the 2D paper representation of a 3D model is difficult to grasp for a non-geo. This could be frustrating for attendees, who’d often comment they wish they had X-ray vision to see through the ground surface to picture geological relationships. 

If only there was an app that would allow a geologist to view their desk-based 3D geological model live in the field via their mobile phone: one that would allow them to see a particular geological unit; any particular boreholes drilled and even the option to understand how deep beneath their feet a particular layer is...so we thought we’d try!

## What it does

Using augmented reality, this mobile app for Android allows pre-existing georeferenced 3D geological surfaces to be visualised in a 3D space beneath the ground in real time. It has the option to view specific geological layers, and to view boreholes which have intersected them. Depth perception is enhanced to allow user greater understanding of geological geometries at depth with relation to their position. A particular geological unit can be measured from the user to get an estimate for how deep that unit is beneath their location. 

## How we built it

ArcGISPro: set up feature classes for each geological layer and share to ArcGISOnline. 

Java Runtime: make geological surfaces as TINS from points. Save as feature layers to ArcGISOnline to be accessed as a service in mobile.  

Android Runtime: Access geological surfaces via service feature tables and display them via a scene view using ARCore, Esri Augmented Reality API. 

