
def router = otp.getRouter()

// Create a default request for a given time
def req = otp.createRequest()
req.setDateTime(2013, 6, 15, 10, 00, 00)
req.setMaxTimeSec(3600)

// Create a regular and rectangular n x n grid of points
def grid = otp.createSyntheticRasterPopulation(47.04, 47.07, -0.90, -0.85, 10, 10)
// Load a CSV population file
def lycees = otp.loadCSVPopulation("lycees.csv")

// For each point of the synthetic grid
for (origin in grid) {

	// Set the origin of the request to this point and run a search
	req.setOrigin(origin)
	def spt = router.plan(req)

	// Evaluate times for all lycees population
	def times = spt.evalTime(lycees)
	// Find the time to nearest lycee
	def minTime = times.min()

	// Find the number of lycee nearer than 30mn
	def n = times.findAll { it < 1800 }.size()

	// TODO: Save the results in an extended result set
	printf("%s: %d %d\n", origin, minTime, n)

}
