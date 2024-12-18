import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { __UCS_GRAPHQL_URL__ } from '../../api-endpoints';
import { AVAILABLE_ALARM_COLLECTION_TYPES_QUERY, FETCH_ALARM_METRICS_QUERY } from '../../graphQL/graphqlQueries';
import LoadingButton from "@mui/lab/LoadingButton";
import { LineChart } from '@mui/x-charts/LineChart';
import { AxisConfig, ChartsXAxisProps } from '@mui/x-charts';
import { Box, FormControl, Grid, InputLabel, MenuItem, Select, SelectChangeEvent, Breadcrumbs, Link } from '@mui/material';
import { AdapterDayjs } from '@mui/x-date-pickers/AdapterDayjs';
import { LocalizationProvider } from '@mui/x-date-pickers/LocalizationProvider';
import dayjs, { Dayjs } from 'dayjs';
import { DatePicker } from "@mui/x-date-pickers/DatePicker";
import { useTheme } from "@mui/material/styles";
import NavigateNextIcon from '@mui/icons-material/NavigateNext';
import HomeIcon from '@mui/icons-material/Home';
import { useSnackbar } from '../../utils/SnackbarContext';

// Define types for data structure
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

// Chart dimensions
const chartHeight = 450;
const childChartHeight = 410;

const ParentChart = ({
  data,
  onDrillDown,
  setIsChildChartDisplayed
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

  return (
    <Box width="100%">
      <LineChart
        series={[
          {
            id: 'totalNumberOfFiles',
            dataKey: 'totalNumberOfFiles',
            label: 'Number of Files',
            data: transformedData.map(item => item.totalNumberOfFiles),
          },
          {
            id: 'totalSizeOfFilesBytes',
            dataKey: 'totalSizeOfFilesBytes',
            label: 'Size of Files (Bytes)',
            data: transformedData.map(item => item.totalSizeOfFilesBytes),
          }
        ]}
        xAxis={[
          {
            data: transformedData.map(item => item.collectionDate),
            id: 'axis1',
            dataKey: 'collectionDate',
            scaleType: "point",
            label: "Collected Date",
          } as AxisConfig<'point', string, ChartsXAxisProps>
        ]}
        height={chartHeight}
        onAxisClick={(event, d) => {
          if (d && d.axisValue) {
            setIsChildChartDisplayed(true);
            onDrillDown(String(d.axisValue));
          }
        }}
      />
    </Box>
  );
};

const ChildChart = ({ date, data }: { date: string; data: DetailedDataPoint[] }) => {
  const theme = useTheme();

  return (
    <Box width="100%">
      <LineChart
        series={[
          {
            id: 'numberOfFiles',
            dataKey: 'numberOfFiles',
            label: 'Number of Files',
            data: data.map(item => item.numberOfFiles),
          },
          {
            id: 'sizeOfFilesBytes',
            dataKey: 'sizeOfFilesBytes',
            label: 'Size of Files (Bytes)',
            data: data.map(item => item.sizeOfFilesBytes),
          }
        ]}
        xAxis={[
          {
            data: data.map(item => item.time),
            id: 'axis2',
            dataKey: 'time',
            scaleType: "point",
            label: `Collected Time on ${date}`,
          } as AxisConfig<'point', string, ChartsXAxisProps>
        ]}
        height={childChartHeight}
      />
    </Box>
  );
};

const HealthMetricsDashboard: React.FC = () => {
  const theme = useTheme();
  const { showSnackbar } = useSnackbar();
  const [collectionTypes, setCollectionTypes] = useState<any[]>([]);
  const [selectedCollectionType, setSelectedCollectionType] = useState<string>('');
  const [fromDate, setFromDate] = useState<Dayjs>(dayjs().subtract(1, 'month'));
  const [toDate, setToDate] = useState<Dayjs | null>(dayjs());
  const [loading, setLoading] = useState(false);
  const [responseData, setResponseData] = useState<DataPoint[]>([]);
  const [viewStack, setViewStack] = useState<string[]>([]);
  const [isChildChartDisplayed, setIsChildChartDisplayed] = useState(false);

  useEffect(() => {
    const fetchCollectionTypes = async () => {
      try {
        const response = await axios.post(__UCS_GRAPHQL_URL__, {
          query: AVAILABLE_ALARM_COLLECTION_TYPES_QUERY,
        });
        setCollectionTypes(response.data?.data?.availableAlarmCollectionTypes || []);
      } catch (error) {
        showSnackbar('Error fetching collection types!');
        console.error(error);
      }
    };
    fetchCollectionTypes();
  }, []);

  const handleFetchClick = async () => {
    setLoading(true);
    const formattedFromDate = dayjs(fromDate).format('YYYY-MM-DD');
    const formattedToDate = dayjs(toDate).format('YYYY-MM-DD');

    try {
      const response = await axios.post(__UCS_GRAPHQL_URL__, {
        query: FETCH_ALARM_METRICS_QUERY,
        variables: { collectionType: selectedCollectionType, fromDate: formattedFromDate, toDate: formattedToDate },
      });
      setResponseData(response.data?.data?.alarmMetrics || []);
    } catch (error) {
      showSnackbar("Error fetching data");
    } finally {
      setLoading(false);
    }
  };

  const currentView = viewStack[viewStack.length - 1];
  const currentData = responseData.find(item => item.date === currentView)?.trendsDrillDownList || [];

  return (
    <Box>
      <Grid container spacing={4}>
        <Grid item xs={12} sm={6}>
          <FormControl fullWidth>
            <InputLabel>Collection Type</InputLabel>
            <Select
              value={selectedCollectionType}
              onChange={e => setSelectedCollectionType(e.target.value)}
            >
              {collectionTypes.map((type, index) => (
                <MenuItem key={index} value={type.type}>{type.type}</MenuItem>
              ))}
            </Select>
          </FormControl>
        </Grid>
        <Grid item xs={12} sm={3}>
          <LocalizationProvider dateAdapter={AdapterDayjs}>
            <DatePicker value={fromDate} onChange={newDate => setFromDate(newDate || dayjs())} />
          </LocalizationProvider>
        </Grid>
        <Grid item xs={12} sm={3}>
          <LocalizationProvider dateAdapter={AdapterDayjs}>
            <DatePicker value={toDate} onChange={newDate => setToDate(newDate)} />
          </LocalizationProvider>
        </Grid>
        <Grid item xs={12}>
          <LoadingButton onClick={handleFetchClick} loading={loading}>
            Fetch Data
          </LoadingButton>
        </Grid>
      </Grid>

      {!isChildChartDisplayed && (
        <ParentChart
          data={responseData}
          onDrillDown={date => setViewStack([...viewStack, date])}
          setIsChildChartDisplayed={setIsChildChartDisplayed}
        />
      )}

      {isChildChartDisplayed && (
        <ChildChart date={currentView} data={currentData} />
      )}

      {viewStack.length > 0 && (
        <Breadcrumbs separator={<NavigateNextIcon />}>
          <Link onClick={() => setIsChildChartDisplayed(false)}>
            <HomeIcon />
          </Link>
          {viewStack.map((date, index) => (
            <Link key={index} onClick={() => setViewStack(viewStack.slice(0, index + 1))}>
              {date}
            </Link>
          ))}
        </Breadcrumbs>
      )}
    </Box>
  );
};

export default HealthMetricsDashboard;