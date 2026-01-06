import React, { useEffect, useState } from "react";
import {
  Container, Typography, Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Paper, Button, Chip, AppBar, Toolbar, IconButton,
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import SecurityIcon from "@mui/icons-material/Security";
import axios from "axios";
import Swal from "sweetalert2"; 

const API_URL = "http://localhost:8080/api";

function App() {
  const [reports, setReports] = useState([]);
  const [loading, setLoading] = useState(true);
  
  // 🔥 নতুন: যেসব আইডি আমরা এইমাত্র ভেরিফাই করলাম, সেগুলোর লিস্ট
  // সার্ভার স্লো হলেও এটি বাটনকে "Verified" ধরে রাখবে।
  const [verifiedIds, setVerifiedIds] = useState(new Set());

  // ১. ডাটা লোড করা
  const fetchReports = async () => {
    try {
      const response = await axios.get(`${API_URL}/reports?t=${new Date().getTime()}`);
      setReports(response.data);
      setLoading(false);
    } catch (error) {
      console.error("Error fetching data:", error);
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
    const interval = setInterval(fetchReports, 5000);
    return () => clearInterval(interval);
  }, []);

  // ২. স্ট্যাটাস আপডেট (Sticky Logic সহ)
  const handleUpdateStatus = async (id) => {
    if (!id) return;

    // A. Sticky Set-এ আইডি যোগ করা (যাতে সার্ভার রিফ্রেশ দিলেও এটা চেঞ্জ না হয়)
    setVerifiedIds((prev) => new Set(prev).add(id));

    // B. লোকাল UI আপডেট (তাৎক্ষণিক)
    setReports((prevReports) =>
      prevReports.map((report) =>
        report.id === id 
          ? { ...report, aiResult: "Verified Scam ✅", status: "Verified Scam ✅" } 
          : report
      )
    );

    try {
      // C. সার্ভারে পাঠানো
      await axios.put(`${API_URL}/report/${id}?status=Verified Scam ✅`);

      // D. সাকসেস মেসেজ
      Swal.fire({
        title: "Verified!",
        text: "Report marked as Verified Scam.",
        icon: "success",
        timer: 1500,
        showConfirmButton: false
      });

      // আমরা এখানে fetchReports() কল করব না, কারণ অটো-রিফ্রেশ এমনিতেই চলছে।

    } catch (error) {
      console.error("Update failed:", error);
      Swal.fire("Error", "Server update failed, but UI updated locally.", "warning");
    }
  };

  // ৩. ডিলিট ফাংশন
  const handleDelete = async (id) => {
    if (!id) return;

    Swal.fire({
      title: "Are you sure?",
      text: "You won't be able to revert this!",
      icon: "warning",
      showCancelButton: true,
      confirmButtonColor: "#d33",
      cancelButtonColor: "#3085d6",
      confirmButtonText: "Yes, delete it!"
    }).then(async (result) => {
      if (result.isConfirmed) {
        // UI থেকে সরানো
        setReports((prev) => prev.filter((r) => r.id !== id));

        try {
          await axios.delete(`${API_URL}/report/${id}`);
          Swal.fire("Deleted!", "Report has been deleted.", "success");
        } catch (error) {
          fetchReports(); 
          Swal.fire("Error!", "Failed to delete from server.", "error");
        }
      }
    });
  };

  return (
    <div style={{ backgroundColor: "#f4f6f8", minHeight: "100vh", width: "100%" }}>
      {/* Navbar */}
      <AppBar position="static" sx={{ backgroundColor: "#1a237e" }}>
        <Toolbar>
          <IconButton edge="start" color="inherit" aria-label="menu" sx={{ mr: 2 }}>
            <SecurityIcon />
          </IconButton>
          <Typography variant="h6" component="div" sx={{ flexGrow: 1, fontWeight: "bold" }}>
            CyberAware Admin Panel
          </Typography>
        </Toolbar>
      </AppBar>

      {/* Main Content */}
      <Container maxWidth={false} sx={{ mt: 4, mb: 4, px: 3 }}>
        <Typography variant="h4" gutterBottom sx={{ fontWeight: "bold", color: "#1a237e" }}>
          🕵️‍♂️ Incoming Scam Reports
        </Typography>

        {loading ? (
          <Typography variant="h6">Loading reports...</Typography>
        ) : (
          <TableContainer component={Paper} sx={{ boxShadow: 3, borderRadius: 2, overflowX: "auto" }}>
            <Table sx={{ minWidth: 650 }} aria-label="scam reports">
              <TableHead sx={{ backgroundColor: "#eeeeee" }}>
                <TableRow>
                  <TableCell sx={{ fontWeight: "bold" }}>ID</TableCell>
                  <TableCell sx={{ fontWeight: "bold" }}>Sender</TableCell>
                  <TableCell sx={{ fontWeight: "bold", width: "40%" }}>Message</TableCell>
                  <TableCell sx={{ fontWeight: "bold" }}>Status</TableCell>
                  <TableCell sx={{ fontWeight: "bold" }} align="center">Actions</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {reports.length === 0 ? (
                  <TableRow><TableCell colSpan={5} align="center">No reports found.</TableCell></TableRow>
                ) : (
                  reports.map((row, index) => {
                    
                    // 🔥 লজিক: সার্ভার থেকে "Verified" আসুক অথবা আমাদের Sticky List-এ থাকুক
                    const currentStatus = row.aiResult || row.ai_result || row.status || "Pending";
                    const isServerVerified = currentStatus.includes("Verified") || currentStatus.includes("Scam");
                    
                    // আসল ম্যাজিক এখানে:
                    const isVerified = isServerVerified || verifiedIds.has(row.id);

                    return (
                      <TableRow key={row.id || index} sx={{ "&:hover": { backgroundColor: "#f5f5f5" } }}>
                        <TableCell>{row.id}</TableCell>
                        <TableCell sx={{ whiteSpace: "nowrap" }}>{row.sender}</TableCell>
                        <TableCell sx={{ wordBreak: "break-word", whiteSpace: "normal", maxWidth: "300px" }}>
                          {row.message}
                        </TableCell>

                        <TableCell>
                          <Chip
                            // যদি আমরা ভেরিফাই করে থাকি, তবে জোর করে "Verified" দেখাব
                            label={isVerified ? "Verified Scam ✅" : currentStatus}
                            color={isVerified ? "success" : "warning"}
                            variant="outlined"
                            size="small"
                          />
                        </TableCell>

                        <TableCell align="center" sx={{ minWidth: "220px" }}>
                          {isVerified ? (
                            <Chip 
                              icon={<CheckCircleIcon />} 
                              label="Verified ✅" 
                              color="success" 
                              variant="filled"
                              sx={{ fontWeight: "bold" }}
                            />
                          ) : (
                            <Button
                              variant="contained" color="success" size="small"
                              startIcon={<CheckCircleIcon />} sx={{ mr: 1 }}
                              onClick={() => handleUpdateStatus(row.id)}
                            >
                              Verify
                            </Button>
                          )}

                          <Button
                            variant="outlined" color="error" size="small"
                            startIcon={<DeleteIcon />} sx={{ ml: 1 }}
                            onClick={() => handleDelete(row.id)}
                          >
                            Delete
                          </Button>
                        </TableCell>
                      </TableRow>
                    );
                  })
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}
      </Container>
    </div>
  );
}

export default App;