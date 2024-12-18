import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { __UCS_GRAPHQL_URL__ } from '../../api-endpoints';
import { UNIQUE_PROJECT_QUERY, GET_HEALTH_METRICS_TRENDS_DATA } from '../../graphQL/graphqlQueries';
import LoadingButton from "@mui/lab/LoadingButton";
import { LineChart } from '@mui/x-charts/LineChart';
import { AxisConfig, ChartsXAxisProps } from '@mui/x-charts';
import { Box, FormControl, Grid, InputLabel, MenuItem, Select, SelectChangeEvent, TextField, Autocomplete, Button, Breadcrumbs, Link, Typography } from '@mui/material';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import dayjs, { Dayjs } from 'dayjs';
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { useTheme } from "@mui/material/styles";
import { useSnackbar } from '../../utils/SnackbarContext';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import HomeIcon from '@mui/icons-material/Home';

const generateLast10Dates = () => {
    const dates = [];
    for (let i = 0; i < 10; i++) {
        dates.push(dayjs().subtract(i, 'day').format('YYYY-MM-DD'));
    }
    return dates;
};

interface HealthMetricDataPoint {
    date: string;
    heartRate: number;
    bloodPressure: number;
    trendsDrillDownList: DetailedHealthMetricDataPoint[];
}

interface DetailedHealthMetricDataPoint {
    time: string;
    heartRate: number;
    bloodPressure: number;
}

const chartHeight = 450;
const childChartHeight = 410;

const ParentChart = ({ data, onDrillDown, setIsChildChartDisplayed }: { data: HealthMetricDataPoint[], onDrillDown: (date: string) => void, setIsChildChartDisplayed: (value: boolean) => void }) => {
    const theme = useTheme();

    const transformedData = data.map(item => ({
        collectionDate: item.date,
        heartRate: item.heartRate,
        bloodPressure: item.bloodPressure,
    }));

    const lineChartParams = {
        series: [
            {
                id: 'heartRate',
                datakey: 'heartRate',
                label: 'Heart Rate',
                data: transformedData.map(item => item.heartRate),
            },
            {
                id: 'bloodPressure',
                datakey: 'bloodPressure',
                label: 'Blood Pressure',
                data: transformedData.map(item => item.bloodPressure),
            }
        ],
        xAxis: [{
            data: transformedData.map(item => item.collectionDate),
            id: 'axis1',
            dataKey: 'collectionDate',
            scaleType: "point",
            label: "Collected Date",
            tickLabelStyle: {
                angle: -25,
                textAnchor: 'end',
                fontSize: 10,
            },
            labelStyle: { transform: "translateY(30px)" },
            tickPlacement: 'middle', tickLabelPlacement: 'middle',
        } as AxisConfig<'point', string, ChartsXAxisProps>],
        height: chartHeight,
        margin: {
            left: 60,
            right: 10,
            top: 20,
            bottom: 80,
        },
        colors: theme.palette.distinctLightPalette
    };

    return (
        <Box width="100%">
            <LineChart
                {...lineChartParams}
                onAxisClick={(event, d) => {
                    if (d && d.axisValue) {
                        setIsChildChartDisplayed(true);
                        onDrillDown(String(d.axisValue));
                    } else {
                        console.error('collectionDate not found in data point');
                    }
                }}
            />
        </Box>
    );
};

const ChildChart = ({ date, data }: { date: string, data: DetailedHealthMetricDataPoint[] }) => {
    const theme = useTheme();
    const detailedLineChartsParams = {
        series: [
            {
                id: 'heartRate',
                datakey: 'heartRate',
                label: 'Heart Rate',
                data: data.map(item => item.heartRate)
            },
            {
                id: 'bloodPressure',
                datakey: 'bloodPressure',
                label: 'Blood Pressure',
                data: data.map(item => item.bloodPressure)
            }
        ],
        xAxis: [{
            data: data.map(item => item.time),
            id: 'axis2',
            dataKey: 'time',
            scaleType: "point",
            label: "Collected Time on " + date,
        } as AxisConfig<'point', string, ChartsXAxisProps>],
        height: childChartHeight,
        colors: theme.palette.distinctLightPalette
    };

    return (
        <Box width="100%">
            <LineChart {...detailedLineChartsParams} />
        </Box>
    );
};

const HealthMetricsDashboard: React.FC = () => {
    const theme = useTheme();
    const [projectList, setProjectList] = useState<any[]>([]);
    const [selectedProject, setSelectedProject] = useState<any>(null);
    const [fromDate, setSelectedFromDate] = useState<Dayjs>(dayjs().subtract(1, 'month'));
    const [toDate, setSelectedToDate] = useState<Dayjs | null>(dayjs());
    const [loading, setLoading] = useState<boolean>(false);
    const [dataFetched, setDataFetched] = useState<boolean>(false);
    const [responseData, setResponseData] = useState<HealthMetricDataPoint[]>([]);
    const [viewStack, setViewStack] = useState<string[]>([]);
    const [isChildChartDisplayed, setIsChildChartDisplayed] = useState(false);
    const { showSnackbar } = useSnackbar();
    const last10Dates = generateLast10Dates();

    useEffect(() => {
        const fetchProjectData = async () => {
            try {
                const projectResponse = await axios.post(__UCS_GRAPHQL_URL__, {
                    query: UNIQUE_PROJECT_QUERY,
                });
                if (projectResponse.data?.data?.uniqueUCSPProjects && projectResponse.data.data.uniqueUCSPProjects.length > 0) {
                    setProjectList(projectResponse.data.data.uniqueUCSPProjects);
                }
            } catch (error) {
                showSnackbar('Error fetching project data!');
                console.error('Error fetching project data', error);
            }
        };
        fetchProjectData();
    }, []);

    const handleProjectChange = async (options: any) => {
        setSelectedProject(options);
    };

    const handleReset = () => {
        setSelectedProject(null);
        setSelectedFromDate(dayjs().subtract(1, 'month'));
        setSelectedToDate(dayjs());
        setResponseData([]);
        setViewStack([]);
        setIsChildChartDisplayed(false);
        setDataFetched(false);
    };

    const handleFetchClick = async () => {
        setLoading(true);
        setResponseData([]);
        const formattedFromDate = dayjs(fromDate).format('YYYY-MM-DD');
        const formattedToDate = dayjs(toDate).format('YYYY-MM-DD');

        const data = {
            projectID: selectedProject?.id,
            fromDate: formattedFromDate,
            toDate: formattedToDate
        };

        try {
            const response = await axios.post(__UCS_GRAPHQL_URL__, {
                query: GET_HEALTH_METRICS_TRENDS_DATA,
                variables: data,
            });

            if (response.data?.data?.healthMetricsTrends && response.data.data.healthMetricsTrends.length > 0) {
                let data = response.data.data.healthMetricsTrends || [];
                setResponseData(data);
            } else {
                showSnackbar("No Data Found!");
            }
            setDataFetched(true);
        } catch (error) {
            showSnackbar("Error While fetching the Data from the Server");
            console.error('Error fetching data', error);
        } finally {
            setLoading(false);
        }
    };

    const handleDrillDown = (date: string) => {
        setViewStack([...viewStack, date]);
    };

    const handleBreadcrumbClick = (index: number) => {
        setViewStack(viewStack.slice(0, index + 1));
    };

    const currentView = viewStack[viewStack.length - 1];
    const currentData = responseData.find(item => item.date === currentView)?.trendsDrillDownList || [];


    const isButtonDisabled = () => {
        return !selectedProject || !fromDate || !toDate;
    };

    return (
        <Box sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            boxSizing: 'border-box',
        }}>
            <Grid container spacing={4} alignItems="flex-end">
                <Grid item xs={12} sm={6} md>
                    <Autocomplete
                        value={selectedProject}
                        onChange={(e, options) => handleProjectChange(options)}
                        loading={loading}
                        disablePortal
                        options={projectList}
                        getOptionLabel={(option) => option.name}
                        renderInput={(params: any) => <TextField {...params} label="Project" placeholder="Project Name" />}
                    />
                </Grid>
                <Grid item xs={12} sm={6} md>
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <DatePicker
                            label="Start Date"
                            sx={{ width: '100%', height: '100%' }}
                            value={fromDate}
                            onChange={(newValue: Dayjs | null) => {
                                setSelectedFromDate(newValue || dayjs());
                                if (newValue && toDate && newValue.isAfter(toDate)) {
                                    setSelectedToDate(null);
                                }
                            }}
                        />
                    </LocalizationProvider>
                </Grid>
                <Grid item xs={12} sm={6} md>
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <DatePicker
                            label="End Date"
                            sx={{ width: '100%', height: '100%' }}
                            value={toDate}
                            minDate={fromDate}
                            onChange={(newValue) => setSelectedToDate(newValue)}
                        />
                    </LocalizationProvider>
                </Grid>
                <Grid item xs={2} sm={2} md={2} sx={{ display: 'flex' }}>
                    <Button variant="contained" onClick={handleReset} color="primary" sx={{ marginRight: 2 }}>Reset</Button>
                    <LoadingButton onClick={handleFetchClick} loading={loading} loadingIndicator="Fetching…" variant="contained"
                        disabled={isButtonDisabled()}>
                        <span>Fetch</span>
                    </LoadingButton>
                </Grid>
            </Grid>

            {dataFetched && !loading && (
                <Box sx={{ mt: 4, width: '100%', borderRadius: 2, border: '1px solid #e0e0e0', bgcolor: 'white' }}>

                    {isChildChartDisplayed && responseData.length > 0 && (
                        <Breadcrumbs aria-label="breadcrumb" separator={<NavigateNextIcon fontSize="small" />} sx={{ padding: '1%' }}>
                            <Link underline="hover" sx={{ display: 'flex', alignItems: 'center', cursor: 'pointer', '&:hover': { textDecoration: 'underline !important', color: '#1976d2' } }} onClick={() => handleBreadcrumbClick(0)}>
                                <HomeIcon fontSize="small" sx={{ marginRight: 1 }} />Home
                            </Link>
                            {viewStack.map((view, index) => (
                                <Link key={index} underline="hover" sx={{ cursor: 'pointer' }} onClick={() => handleBreadcrumbClick(index)}>
                                    {view}
                                </Link>
                            ))}
                        </Breadcrumbs>
                    )}

                    <Box sx={{ width: '100%', padding: '20px' }}>
                        {isChildChartDisplayed && currentData.length > 0 ? (
                            <ChildChart date={currentView} data={currentData} />
                        ) : (
                            <ParentChart data={responseData} onDrillDown={handleDrillDown} setIsChildChartDisplayed={setIsChildChartDisplayed} />
                        )}
                    </Box>
                </Box>
            )}
        </Box>
    );
};

export default HealthMetricsDashboard;