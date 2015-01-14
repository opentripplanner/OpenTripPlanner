
def router = otp.getRouter()

// Create a default request for a given time
def req = otp.createRequest()
req.setDateTime(2015, 1, 15, 10, 00, 00)
req.setMaxTimeSec(1800)

// Create a regular and rectangular n x n grid of points
def grid = otp.createGridPopulation(43.201, 43.359, 5.284, 5.483, 20, 20)

// Load population files, CSV or GeoTIFF
def colleges = otp.loadCSVPopulation('colleges.csv', 'Y', 'X')
def population = otp.loadCSVPopulation('insee.csv', 'y', 'x')

// Create a CSV output
grid30Csv = otp.createCSVOutput()
grid30Csv.setHeader([ 'lat', 'lon', 'min_college_time', 'n_colleges_30', 'n_pop_30' ])

// For each point of the synthetic grid
for (origin in grid) {

	printf("Processing: %s\n", origin)
	// Set the origin of the request to this point and run a search
	req.setOrigin(origin)
	def spt = router.plan(req)
	if (spt == null) continue

	// Evaluate times for all colleges
	def res = spt.eval(colleges)
	// Find the time to nearest college
	// TODO def minTime = res.min()
	def minTime = -1

	// Find the number of colleges nearer than 30mn
	def nCollege30 = res.findAll { it.getTime() < 1800 }.size()
	// TODO pop
	def nPop30 = res.findAll { it.getTime() < 1800 }.size();

	// Add a new row of result in the CSV output
	grid30Csv.addRow([ spt.getSnappedOrigin().getLat(), spt.getSnappedOrigin().getLon(),
		minTime, nCollege30, nPop30 ])
}

grid30Csv.save("grid30b.csv")
