
# Get the default router
# Could also be called: router = otp.getRouter('paris')
router = otp.getRouter()

# Create a default request for a given time
req = otp.createRequest()
req.setDateTime(2015, 1, 15, 10, 00, 00)
req.setMaxTimeSec(1800)

# Create a regular and rectangular n x n grid of points
grid = otp.createGridPopulation(43.201, 43.359, 5.284, 5.483, 20, 20)

# Load population files, CSV or GeoTIFF
colleges = otp.loadCSVPopulation('colleges.csv', 'Y', 'X')
population = otp.loadCSVPopulation('insee.csv', 'y', 'x')

# Create a CSV output
grid30Csv = otp.createCSVOutput()
grid30Csv.setHeader([ 'lat', 'lon', 'min_college_time', 'n_colleges_30', 'n_pop_30' ])

# For each point of the synthetic grid
for origin in grid:

	print "Processing: ", origin
	# Set the origin of the request to this point and run a search
	req.setOrigin(origin)
	spt = router.plan(req)
	if spt is None: continue

	# Evaluate the SPT for all colleges
	res = spt.eval(colleges)
	# Find the time to nearest college
	if len(res) == 0:	minTime = -1
	else:			minTime = min([ r.getTime() for r in res ])
	# Find the number of colleges < 30mn
	nCollege30 = sum([ 1 for r in res if r.getTime() < 1800 ])

	# Evaluate the SPT for the population
	res = spt.eval(population)
	# Sum population < 30mn
	nPop30 = sum([ r.getIndividual().getFloatData('ind') for r in res if r.getTime() < 1800 ])

	# Add a new row of result in the CSV output
	grid30Csv.addRow([ spt.getSnappedOrigin().getLat(), spt.getSnappedOrigin().getLon(),
		minTime, nCollege30, nPop30 ])

# Save the result
grid30Csv.save('grid30.csv')
