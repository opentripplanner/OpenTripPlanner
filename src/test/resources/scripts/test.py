
# Get the default router
# Could also be called: router = otp.getRouter('paris')
router = otp.getRouter()

# Create a default request for a given time
req = otp.createRequest()
req.setDateTime(2013, 6, 15, 10, 00, 00)
req.setMaxTimeSec(3600)

# Create a regular and rectangular n x n grid of points
grid = otp.createSyntheticRasterPopulation(47.04, 47.07, -0.90, -0.85, 10, 10)
# Load a CSV population file
lycees = otp.loadCSVPopulation('lycees.csv')

# For each point of the synthetic grid
for origin in grid:

	# Set the origin of the request to this point and run a search
	req.setOrigin(origin)
	spt = router.plan(req)

	# Evaluate times for all lycees population
	times = spt.evalTime(lycees)
	# Find the time to nearest lycee
	minTime = min(times)

	# Find the number of lycee nearer than 30mn
	n = len([1 for time in times if time < 1800])

	# TODO: Save the results in an extended result set
	print origin, minTime, n
