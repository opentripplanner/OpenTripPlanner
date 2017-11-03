# Routing

This is a list of articles, dissertations, and books that have inspired and informed both the existing OTP routing engine and some ongoing experiments.

Currently, OTP uses a single time-dependent (as opposed to time-expanded) graph that contains both street and transit networks. Walk-only and bicycle-only trips are generally planned using the A* algorithm with a Euclidean heuristic. Walk+Transit or Bike+Transit trips are planned using Multiobjective A* and the Tung-Chew heuristic (a graph grown backward from the destination providing a lower bound on aggregate weight) for queue ordering.

Please feel free to add references or summaries!

## Path Search Speedup Techniques

- Bast, Hannah. Car or public transport -- two worlds. (2009)<br>
Explains how car routing is different from schedule-based public transport routing.<br>
http://www.mpi-inf.mpg.de/~bast/papers/car_or_public_transport.pdf

- Delling, Daniel. Engineering and augmenting route planning algorithms. (2009, dissertation)
<BR>Overview, including time-dependent and Pareto shortest paths.
<BR>http://i11www.ira.uka.de/extra/publications/d-earpa-09.pdf

- Delling, Sanders, Schultes, and Wagner. Engineering Route-Planning Algorithms. (2009)
<BR>Overview.
<BR>http://i11www.ira.uka.de/extra/publications/dssw-erpa-09.pdf

- Delling and Wagner. Time-Dependent Route Planning. (2009)
<BR>Overview.
<BR>http://i11www.iti.uni-karlsruhe.de/extra/publications/dw-tdrp-09.pdf

- Delling and Wagner. Landmark-Based Routing in Dynamic Graphs. (2008)
<BR>http://i11www.ira.uka.de/extra/publications/dw-lbrdg-07.pdf

- Bauer, Delling, Sanders, Schultes, and Wagner. Combining Hierarchical and Goal-Directed
Speed-Up Techniques for Dijkstra’s Algorithm. (2008)
<BR>http://algo2.iti.kit.edu/download/bdsssw-chgds-10.pdf

- Bauer and Delling. SHARC: Fast and Robust Unidirectional Routing. (2009)
<BR>**SH** ortcuts + **ARC** flags. Can be combined with ALT.
<BR>http://www.siam.org/proceedings/alenex/2008/alx08_02bauerr.pdf

- Delling, Daniel. Time-Dependent SHARC-Routing. (2008)
<BR>http://i11www.iti.uni-karlsruhe.de/extra/publications/d-tdsr-09.pdf

- Goldberg, Kaplan, and Werneck. Reach for A∗: Efficient Point-to-Point Shortest Path Algorithms. (2005)
<BR>http://avglab.com/andrew/pub/msr-tr-2005-132.pdf

## Multi-objective Pareto Shortest Paths

- Das and Dennis. Drawbacks of minimizing weighted sums of objectives for Pareto set generation in multicriteria optimization problems. (1997)

- Müller-Hannemann and Schnee. Finding All Attractive Train Connections by Multi-criteria Pareto Search. (2007)
<BR>Deutsche Bahn information system. Does not account for on-street travel.

- Mandow & Pérez de la Cruz. A New Approach to Multiobjective A* Search. (2005)
<BR>NAMOA*
<BR>http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.97.8780&rep=rep1&type=pdf

- Mandow & Pérez de la Cruz. Multiobjective A* search with consistent heuristics. (2008)
<BR>NAMOA*

- Machuca, Mandow and Pérez de la Cruz. Evaluation of Heuristic Functions for Bicriterion Shortest Path Problems. (2009)
<BR>Evaluates heuristics from Tung & Chew (1992) versus lexicographical ordering of priority queue.
<BR>http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.160.4715&rep=rep1&type=pdf

- Perny and Spanjaard. Near Admissible Algorithms for Multiobjective Search. (2009)
<BR>Discusses relaxed Pareto dominance (Epsilon-dominance) and its use in Multi-objective A*. This a scheme for approximating the entire pareto-optimal solution set that allows time and space complexity polynomial in the number of nodes.
<BR>http://www-desir.lip6.fr/publications/pub_1052_1_ECAI08.pdf

- Tung and Chew. A multicriteria Pareto-optimal path algorithm. (1992)

- Delling and Wagner. Pareto Paths with SHARC. (2009)
<BR>http://i11www.iti.uni-karlsruhe.de/extra/publications/dw-pps-09.pdf

## Resource-constrained Routing

- Dumitrescu & Boland. Improved Preprocessing, Labeling and Scaling Algorithms for the Weight-Constrained Shortest Path Problem. (2003)
<BR>Comparison of scaling and label-setting methods.

- Ziegelmann, Mark. Constrained Shortest Paths and Related Problems. (2001, dissertation)
<BR>http://scidok.sulb.uni-saarland.de/volltexte/2004/251/pdf/MarkZiegelmann_ProfDrKurtMehlhorn.pdf

## Contraction and Transfer Patterns

- Geisberger, Robert. Contraction Hierarchies: Faster and Simpler Hierarchical Routing in Road Networks. (2008, dissertation)
<BR>http://algo2.iti.kit.edu/documents/routeplanning/geisberger_dipl.pdf

- Geisberger, Robert. Contraction of Timetable Networks with Realistic Tranfers (2010)
<BR>Introduces the "Station Model Graph".
<BR>http://algo2.iti.kit.edu/download/time_table_ch.pdf

- Bast, Carlsson, Eigenwillig, Geisberger Harrelson, Raychev, and Viger. Fast Routing in Very Large Public Transportation Networks Using Transfer Patterns. (2010)
<BR>http://ad.informatik.uni-freiburg.de/files/transferpatterns.pdf/at_download/file


## Timetable-based routing

- Schulz, Frank. Timetable Information and Shortest Paths. (2005, dissertation)
<BR>Excellent reference.
<BR>http://d-nb.info/1001586921/34

## ALT and Metric Embeddings

- Goldberg and Werneck. Computing Point-to-Point Shortest Paths from External Memory. (2005)
<BR>Introduced the ALT algorithm.
<BR>http://www.cs.princeton.edu/courses/archive/spring06/cos423/Handouts/GW05.pdf

- Linial, London, and Rabinovich. The Geometry of Graphs and Some of its Algorithmic Applications. (1995)
<BR>http://pdf.aminer.org/000/798/423/the_geometry_of_graphs_and_some_of_its_algorithmic_applications.pdf

- Hjaltason and Samet. Contractive Embedding Methods for Similarity Searching in Metric Spaces. (2000)
<BR>http://www.cs.umd.edu/~hjs/pubs/metricpruning.pdf

- Potamias, Bonchi, Castillo, and Gionis. Fast Shortest Path Distance Estimation in Large Networks. (2009)
<BR>Briefly discusses the connection between landmark routing and more general research on metric embeddings.
<BR>http://dcommon.bu.edu/xmlui/bitstream/handle/2144/1727/2009-004-shortest-distance-estimation.pdf

## Calibration and Implementation Details

- Wardman, Mark. Public Transport Values of Time. (2004)
<BR>http://eprints.whiterose.ac.uk/2062/1/ITS37_WP564_uploadable.pdf

- A.M. El-Geneidy, K.J. Krizek, M.J. Iacono. Predicting bicycle travel speeds along different facilities using GPS data: a proof of concept model. (2007)<BR>Proceedings of the 86th Annual Meeting of the Transportation Research Board, Compendium of Papers, TRB, Washington, D.C., USA (CD-ROM)

- Chen, Chowdhury, Roche, Ramachandran, Tong. Priority Queues and Dijkstra’s Algorithm.
<BR>Summary: Despite better theoretical complexity for Fibonacci heaps, it is often as good or better to use a binary heap as a priority queue when doing path searches.
<BR>http://www.cs.utexas.edu/users/shaikat/papers/TR-07-54.pdf

## Post-Dijkstra Public Transit Routing

- Delling, Pajor, Werneck. Round-Based Public Transit Routing (2012)
<BR>This is a tabular approach to routing in public transit networks that does not use an (explicit) graph. It is simpler and can outperform classic graph algorithms.
<BR>http://research.microsoft.com/pubs/156567/raptor_alenex.pdf

- Dibbelt, Pajor, Strasser, Wagner. Intriguingly Simple and Fast Transit Routing (2013).
<BR>Introduces the Connection Scan Algorithm (CSA).
<BR>http://www.ecompass-project.eu/sites/default/files/ECOMPASS-TR-021.pdf

- Delling, Katz, and Pajor. Parallel computation of best connections in public transportation networks (2012).
<BR>"In this work, we present a novel algorithm for the one-to-all profile-search problem in public transportation networks. It answers the question for all fastest connections between a given station S and any other station at any time of the day in a single query... two interesting questions arise for time-dependent route planning: compute the best connection for a given departure time and the computation of all best connections during a given time interval (e. g., a whole day). The former is called a time-query, while the latter is called a proﬁle-query."
<BR>http://www.ecompass-project.eu/sites/default/files/ECOMPASS-TR-021.pdf



# Analytics

This is a list of articles about non-passenger-facing applications of multi-modal routing engines (including OTP) in urban planning, public policy, economics, geography etc.

* Busby, Jeffrey R. “Accessibility-Based Transit Planning”. MA thesis. Massachusetts Institute of Technology, 2004. Web. [http://dspace.mit.edu/handle/1721.1/32414]

* Lei, T. L. and R. L. Church. “Mapping transit-based access: integrating GIS, routes and schedules.” International Journal of Geographical Information Science 24.2 (2010): 283–304. Web. [http://www.tandfonline.com/doi/pdf/10.1080/13658810902835404].

* McGurrin, Greczner. Performance Metrics: Calculating Accessibility Using Open Source Software
and Open Data. (2010). Web. [http://amonline.trb.org/12jj6e/12jj6e/1]
An approach for calculating accessibility via both transit and automobiles is presented. OTP is used for the transit portion.

* Tomer, Kneebone, Puentes, and Berube. Missed Opportunity: Transit and Jobs in Metropolitan America. (2011)
<BR>Large-scale comparative public transit accessibility study.
<BR>[http://www.brookings.edu/reports/2011/0512_jobs_and_transit.aspx]

* Huang, Ruihong. "Bridging GTFS and GIS for Computing Transit Level of Service," Association of American Geographers Annual Meeting. (2011)
<BR>Presents data models and techniques to bridge GTFS data and GIS data and facilitate computation of various transit system level of service (TLOS) indicators for U.S. metropolitan areas.
<BR>[http://meridian.aag.org/callforpapers/program/AbstractDetail.cfm?AbstractID=39742]

* Trozzi, Hosseinloo, Gentile2, and Bell. Dynamic Hyperpaths: The Stop Model. (2009)
<BR> [paper 1](https://3907fa3c-a-2a1be95a-s-sites.googlegroups.com/a/sistemaits.com/guidogentile/pubblicazioni/53%20Dynamic%20hyperpaths.%20The%20stop%20model%20%28MTITS2009%29.pdf?attachauth=ANoY7cpnkZHMSC_R-GbTfpeP4ya5sjKasnV_pHqLohYVMS8NzUSXe4JVq0cXWg1JQzv0NdKefTpIwMVH3nqTqKrkt4wHW3pekz_K6t3G5Yp_XVympT3djH2imh4YF3fi-_DtMotl85deftNWD35s7TZBMbmNKwPhQ7db2_qYAQv-EUn2wcnuV7lCjVJW6782I5m7jnMWcge7EdI8WtyrqIn_Om8zykdU7fdkk3UD7EW1JKGcZg9KDyRV5EJ7Xng2ChYMw9bEE7Ef4GMfKSACX783zkIOrTleNJHY1u5fxlKcHvsuiW-HHp4%3D&attredirects=0)
<BR> [paper 2](https://3907fa3c-a-2a1be95a-s-sites.googlegroups.com/a/sistemaits.com/guidogentile/pubblicazioni/68%20Dynamic%20hyperpaths%20in%20transit%20networks.%20The%20stop%20model%20with%20online%20information%20%28IMA2010%29.pdf?attachauth=ANoY7cpohngZT-bg8qZTJarOGNtxxnqLqyRvbeCYcFVQyFGGErW3IQkxjfTFMVHUjxd0LcmFaVg4B9ZWKsuVkEO6h5HKJUQTr8EoHFyfi_CmXqlPWnx8mGhXvCGlA9oZ90C2PoRTiejMpUIW5fwCCkGjonB1kUFLp7YWUB-iqHyZ7uliIOp3Lp6qPec5YYWyOLtK8Z10jRYYf9NPnlzQTKIxu7UF5wc_P0wtvjzkn04oPoJ4OiIJ1S5hcW2rGQ3zodyPnn-a1fry7srESnmK0y3ZjrJhtk2g9CwrTKUkGkgf-jYNmI6QTE3znRlqX9UM6rp5Dnh8HOnE_BHt6e_LkuVALbOCfV5fTQ%3D%3D&attredirects=1)
<BR>Effective frequencies of multiple frequency-based routes operating over common trunks. The passenger's position should be modeled as a superposition of positions along the various lines. Some work must be done here to present reasonable trips.

 
