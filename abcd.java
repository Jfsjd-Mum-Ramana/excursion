import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { __UCS_GRAPHQL_URL__ } from '../../api-endpoints';
import { AVAILABLE_ALARM_COLLECTION_TYPES_QUERY, FETCH_ALARM_METRICS_QUERY } from '../../graphQL/graphqlQueries';
import { LineChart } from '@mui/x-charts/LineChart';
import { AxisConfig, ChartsXAxisProps } from '@mui/x-charts';
import {
  Box,
  FormControl,
  Grid,
  InputLabel,
  MenuItem,
  Select,
  SelectChangeEvent,
  Button,
  Breadcrumbs,
  Link,
  Typography,
} from '@mui/material';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import dayjs, { Dayjs } from 'dayjs';
import { DatePicker } from '@mui/x-date-pickers/DatePicker';
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import HomeIcon from '@mui/icons-material/Home';
import { useTheme } from '@mui/material/styles';
import { useSnackbar } from '../../utils/SnackbarContext';
import LoadingButton from '@mui/lab/LoadingButton';

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
const childChartHeight = 400;

// Parent Chart Component
const ParentChart = ({
  data,
  onDrillDown,
  setIsChildChartDisplayed,
}: {
  data: DataPoint[];
  onDrillDown: (date: string) => void;
  setIsChildChartDisplayed: (value: boolean) => void;
}) => {
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
      },
    ],
    xAxis: [
      {
        data: transformedData.map(item => item.collectionDate),
        id: 'axis1',
        dataKey: 'collectionDate',
        scaleType: 'point',
        label: 'Collected Date',
        tickLabelStyle: {
          angle: -30,
          textAnchor: 'end',
          fontSize: 12,
        },
      } as AxisConfig<'point', string, ChartsXAxisProps>,
    ],
    height: chartHeight,
    margin: {
      left: 70,
      right: 20,
      top: 20,
      bottom: 80,
    },
    colors: theme.palette.primary,
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

// Child Chart Component
const ChildChart = ({ date, data }: { date: string; data: DetailedDataPoint[] }) => {
  const theme = useTheme();

  const detailedLineChartsParams = {
    series: [
      {
        id: 'numberOfFiles',
        datakey: 'numberOfFiles',
        label: 'Number of Files',
        data: data.map(item => item.numberOfFiles),
      },
      {
        id: 'sizeOfFilesBytes',
        datakey: 'sizeOfFilesBytes',
        label: 'Size of Files (Bytes)',
        data: data.map(item => item.sizeOfFilesBytes),
      },
    ],
    xAxis: [
      {
        data: data.map(item => item.time),
        id: 'axis2',
        dataKey: 'time',
        scaleType: 'point',
        label: 'Collected Time',
      } as AxisConfig<'point', string, ChartsXAxisProps>,
    ],
    height: childChartHeight,
    colors: theme.palette.secondary,
  };

  return (
    <Box width="100%">
      <LineChart {...detailedLineChartsParams} />
    </Box>
  );
};

// Main Health Metrics Dashboard
const HealthMetricsDashboard: React.FC = () => {
  const theme = useTheme();
  const { showSnackbar } = useSnackbar();
  const [collectionTypes, setCollectionTypes] = useState<any[]>([]);
  const [selectedCollectionType, setSelectedCollectionType] = useState<string>('');
  const [fromDate, setFromDate] = useState<Dayjs>(dayjs().subtract(1, 'month'));
  const [toDate, setToDate] = useState<Dayjs>(dayjs());
  const [loading, setLoading] = useState<boolean>(false);
  const [dataFetched, setDataFetched] = useState<boolean>(false);
  const [responseData, setResponseData] = useState<DataPoint[]>([]);
  const [viewStack, setViewStack] = useState<string[]>([]);
  const [isChildChartDisplayed, setIsChildChartDisplayed] = useState(false);

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
    }
  };

  useEffect(() => {
    fetchCollectionTypes();
  }, []);

  const handleCollectionTypeChange = (event: SelectChangeEvent<string>) => {
    setSelectedCollectionType(event.target.value);
  };

  const fetchMetrics = async () => {
    setLoading(true);
    const formattedFromDate = dayjs(fromDate).format('YYYY-MM-DD');
    const formattedToDate = dayjs(toDate).format('YYYY-MM-DD');

    try {
      const response = await axios.post(__UCS_GRAPHQL_URL__, {
        query: FETCH_ALARM_METRICS_QUERY,
        variables: {
          collectionType: selectedCollectionType,
          fromDate: formattedFromDate,
          toDate: formattedToDate,
        },
      });

      if (response.data?.data?.alarmMetrics) {
        setResponseData(response.data.data.alarmMetrics);
      } else {
        showSnackbar('No data found!');
      }

      setDataFetched(true);
    } catch (error) {
      showSnackbar('Error fetching data from server');
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

  return (
    <Box>
      <Grid container spacing={2}>
        <Grid item xs={4}>
          <FormControl fullWidth>
            <InputLabel>Collection Type</InputLabel>
            <Select value={selectedCollectionType} onChange={handleCollectionTypeChange}>
              {collectionTypes.map(type => (
                <MenuItem key={type.typeId} value={type.type}>
                  {type.type}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={4}>
          <LocalizationProvider dateAdapter={AdapterDayjs}>
            <DatePicker label="Start Date" value={fromDate} onChange={setFromDate} />
          </LocalizationProvider>
        </Grid>
        <Grid item xs={4}>
          <LocalizationProvider dateAdapter={AdapterDayjs}>
            <DatePicker label="End Date" value={toDate} onChange={setToDate} />
          </LocalizationProvider>
        </Grid>
      </Grid>
      <Button onClick={fetchMetrics}>Fetch Metrics</Button>
      {dataFetched && (
        <ParentChart
          data={responseData}
          onDrillDown={handleDrillDown}
          setIsChildChartDisplayed={setIsChildChartDisplayed}
        />
      )}
      {isChildChartDisplayed && <ChildChart date={currentView} data={currentData} />}
    </Box>
  );
};

export default HealthMetricsDashboard;