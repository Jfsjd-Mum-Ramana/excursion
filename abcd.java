import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { __UCS_GRAPHQL_URL__ } from '../../api-endpoints';
import { AVAILABLE_ALARM_COLLECTION_TYPES_QUERY, FETCH_ALARM_METRICS_QUERY } from '../../graphQL/graphqlQueries';
import LoadingButton from "@mui/lab/LoadingButton";
import { LineChart } from '@mui/x-charts/LineChart';
import { AxisConfig, ChartsXAxisProps } from '@mui/x-charts';
import { Box, FormControl, Grid, InputLabel, MenuItem, Select, SelectChangeEvent, TextField, Button, Breadcrumbs, Link, Typography } from '@mui/material';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import dayjs, { Dayjs } from 'dayjs';
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { useTheme } from "@mui/material/styles";
import { useSnackbar } from '../../utils/SnackbarContext';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import HomeIcon from '@mui/icons-material/Home';

interface DataPoint {
    date: string;
    totalNumberOfFiles: number;
    totalSizeOfFilesBytes: number;
    trendsDrillDownList: DetailedDataPoint[];
}

interface DetailedDataPoint {
    time: string;
    sizeOfFilesBytes: number;
    numberOfFiles: number;
}

const chartHeight = 450;
const childChartHeight = 410;

const ParentChart = ({ data, onDrillDown }: { data: DataPoint[], onDrillDown: (date: string) => void }) => {
    const theme = useTheme();

    const transformedData = data.map(item => ({
        collectionDate: item.date,
        totalNumberOfFiles: item.totalNumberOfFiles,
        totalSizeOfFilesBytes: item.totalSizeOfFilesBytes,
    }));

    const lineChartParams = {
        series: [
            {
                id: 'totalNumberOfFiles',
                datakey: 'totalNumberOfFiles',
                label: 'Number of Files',
                data: transformedData.map(item => item.totalNumberOfFiles),
            },
            {
                id: 'totalSizeOfFilesBytes',
                datakey: 'totalSizeOfFilesBytes',
                label: 'Size of Files (Bytes)',
                data: transformedData.map(item => item.totalSizeOfFilesBytes),
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
                        onDrillDown(String(d.axisValue));
                    } else {
                        console.error('collectionDate not found in data point');
                    }
                }}
            />
        </Box>
    );
};

const ChildChart = ({ date, data }: { date: string, data: DetailedDataPoint[] }) => {
    const theme = useTheme();
    const detailedLineChartsParams = {
        series: [
            {
                id: 'numberOfFiles',
                datakey: 'numberOfFiles',
                label: 'Number of Files',
                data: data.map(item => item.numberOfFiles)
            },
            {
                id: 'sizeOfFilesBytes',
                datakey: 'sizeOfFilesBytes',
                label: 'Size of Files (Bytes)',
                data: data.map(item => item.sizeOfFilesBytes)
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
    const [collectionTypes, setCollectionTypes] = useState<any[]>([]);
    const [selectedCollectionType, setSelectedCollectionType] = useState<string>('');
    const [fromDate, setSelectedFromDate] = useState<Dayjs>(dayjs().subtract(1, 'month'));
    const [toDate, setSelectedToDate] = useState<Dayjs | null>(dayjs());
    const [loading, setLoading] = useState<boolean>(false);
    const [dataFetched, setDataFetched] = useState<boolean>(false);
    const [responseData, setResponseData] = useState<DataPoint[]>([]);
    const [viewStack, setViewStack] = useState<string[]>([]);
    const [isChildChartDisplayed, setIsChildChartDisplayed] = useState(false);
    const { showSnackbar } = useSnackbar();

    useEffect(() => {
        const fetchCollectionTypes = async () => {
            try {
                const response = await axios.post(__UCS_GRAPHQL_URL__, {
                    query: AVAILABLE_ALARM_COLLECTION_TYPES_QUERY,
                });
                if (response.data?.data?.availableAlarmCollectionTypes) {
                    setCollectionTypes(response.data.data.availableAlarmCollectionTypes);
                }
            } catch (error) {
                showSnackbar('Error fetching collection types!');
                console.error('Error fetching collection types', error);
            }
        };
        fetchCollectionTypes();
    }, []);

    const handleCollectionTypeChange = (event: SelectChangeEvent<string>) => {
        setSelectedCollectionType(event.target.value);
    };

    const handleReset = () => {
        setSelectedCollectionType('');
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

        try {
            const response = await axios.post(__UCS_GRAPHQL_URL__, {
                query: FETCH_ALARM_METRICS_QUERY,
                variables: {
                    collectionType: selectedCollectionType,
                    fromDate: formattedFromDate,
                    toDate: formattedToDate
                },
            });

            if (response.data?.data?.alarmMetrics) {
                setResponseData(response.data.data.alarmMetrics);
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
        setIsChildChartDisplayed(true);
        setViewStack([...viewStack, date]);
    };

    const handleBreadcrumbClick = (index: number) => {
        setViewStack(viewStack.slice(0, index + 1));
    };

    const handleHomeClick = () => {
        setIsChildChartDisplayed(false);
        setViewStack([]);
    };

    const currentView = viewStack[viewStack.length - 1];
    const currentData = responseData.find(item => item.date === currentView)?.trendsDrillDownList || [];

    const isButtonDisabled = () => {
        return !selectedCollectionType || !fromDate || !toDate;
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
                    <FormControl variant="outlined" sx={{ width: '100%', height: '100%' }}>
                        <InputLabel id="select-collection-type-label">Collection Type</InputLabel>
                        <Select
                            labelId="select-collection-type-label"
                            id="select-collection-type"
                            label="Collection Type"
                            value={selectedCollectionType}
                            onChange={handleCollectionTypeChange}
                        >
                            {collectionTypes.map((type, i) => (
                                <MenuItem key={type.typeId} value={type.type}>
                                    {type.type}
                                </MenuItem>
                            ))}
                        </Select>
                    </FormControl>
                </Grid>
                <Grid item xs={12} sm={6} md>
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <DatePicker
                            label="Start Date"
                            sx={{ width: '100%', height: '100%' }}
                            value={fromDate}
                            onChange={(newDate: Dayjs | null) => setSelectedFromDate(newDate || dayjs())}
                        />
                    </LocalizationProvider>
                </Grid>
                <Grid item xs={12} sm={6} md>
                    <LocalizationProvider dateAdapter={AdapterDayjs}>
                        <DatePicker
                            label="End Date"
                            sx={{ width: '100%', height: '100%' }}
                            value={toDate}
                            onChange={(newDate: Dayjs | null) => setSelectedToDate(newDate || dayjs())}
                        />
                    </LocalizationProvider>
                </Grid>
                <Grid item xs={12} sm={6} md={2}>
                    <LoadingButton
                        loading={loading}
                        variant="contained"
                        onClick={handleFetchClick}
                        disabled={isButtonDisabled()}
                    >
                        Fetch Data
                    </LoadingButton>
                </Grid>
            </Grid>
            {dataFetched && !isChildChartDisplayed && (
                <>
                    <ParentChart
                        data={responseData}
                        onDrillDown={handleDrillDown}
                    />
                </>
            )}
            {isChildChartDisplayed && (
                <>
                    <ChildChart
                        date={currentView}
                        data={currentData}
                    />
                    <Breadcrumbs
                        separator={<NavigateNextIcon fontSize="small" />}
                        aria-label="breadcrumb"
                        sx={{ margin: "10px" }}
                    >
                        <Link href="/" color="inherit" onClick={handleHomeClick}>
                            <HomeIcon fontSize="small" />
                        </Link>
                        {viewStack.map((date, index) => (
                            <Link
                                key={index}
                                color="inherit"
                                onClick={() => handleBreadcrumbClick(index)}
                            >
                                {date}
                            </Link>
                        ))}
                    </Breadcrumbs>
                </>
            )}
        </Box>
    );
};

export default HealthMetricsDashboard;