package GUI;

import DataBase.BuildingDML;
import DataBase.FloorDML;
import DataBase.RoomDML;
import Properties.Building;
import Properties.Floor;
import Properties.Room;
import Users.Landlord;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class RoomManagementGUI extends JPanel {
    private JTable roomTable;
    private DefaultTableModel tableModel;
    private JTextField roomNumberField, electricCounterField, waterCounterField;
    private JComboBox<String> buildingComboBox, floorComboBox;
    private JButton addButton, updateButton, removeButton, clearButton, refreshButton;
    private RoomDML roomDML;
    private BuildingDML buildingDML;
    private FloorDML floorDML;
    private Landlord landlord;
    private int selectedRoomId = -1;

    public RoomManagementGUI(Landlord landlord) {
        this.landlord = landlord;
        this.roomDML = new RoomDML();
        this.buildingDML = new BuildingDML();
        this.floorDML = new FloorDML();

        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel formPanel = createFormPanel();
        JPanel tablePanel = createTablePanel();
        JPanel buttonPanel = createButtonPanel();

        add(formPanel, BorderLayout.NORTH);
        add(tablePanel, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load initial data
        loadBuildings();
        loadFloors(); // Load floors for the first building (if any)
        loadRoomsData(); // Load all rooms initially
    }

    private JPanel createFormPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        panel.setBorder(BorderFactory.createTitledBorder("Room Details"));

        panel.add(new JLabel("Building:"));
        buildingComboBox = new JComboBox<>();
        buildingComboBox.addItem("All Buildings"); // Add "All Buildings" option
        buildingComboBox.setEditable(false); // Make it selection-only
        buildingComboBox.addActionListener(e -> {
            loadFloors();
            filterRoomsByBuildingAndFloor();
        });
        panel.add(buildingComboBox);

        panel.add(new JLabel("Floor:"));
        floorComboBox = new JComboBox<>();
        floorComboBox.addItem("All Floors"); // Add "All Floors" option
        floorComboBox.setEditable(false); // Make it selection-only
        floorComboBox.addActionListener(e -> filterRoomsByBuildingAndFloor());
        panel.add(floorComboBox);

        panel.add(new JLabel("Room Number:"));
        roomNumberField = new JTextField(10);
        panel.add(roomNumberField);

        panel.add(new JLabel("Electric Counter:"));
        electricCounterField = new JTextField(10);
        panel.add(electricCounterField);

        panel.add(new JLabel("Water Counter:"));
        waterCounterField = new JTextField(10);
        panel.add(waterCounterField);

        return panel;
    }

    private JPanel createTablePanel() {
        String[] columnNames = {"ID", "Building", "Floor", "Room Number", "Electric Counter", "Water Counter", "Occupied", "Tenant"};
        tableModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        roomTable = new JTable(tableModel);
        roomTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        roomTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && roomTable.getSelectedRow() != -1) {
                int row = roomTable.getSelectedRow();
                selectedRoomId = (Integer) tableModel.getValueAt(row, 0);
                Room room = roomDML.getRoomById(selectedRoomId);
                if (room != null) {
                    roomNumberField.setText(room.getRoomNumber());
                    electricCounterField.setText(String.valueOf(room.getCurrentElectricCounter()));
                    waterCounterField.setText(String.valueOf(room.getCurrentWaterCounter()));
                    String buildingName = (String) tableModel.getValueAt(row, 1);
                    String floorNumber = (String) tableModel.getValueAt(row, 2);
                    buildingComboBox.setSelectedItem(buildingName);
                    floorComboBox.setSelectedItem(floorNumber);
                }
                updateButton.setEnabled(true);
                removeButton.setEnabled(true);
            }
        });

        JScrollPane scrollPane = new JScrollPane(roomTable);

        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Rooms"));

        refreshButton = new JButton("Refresh List");
        refreshButton.addActionListener(e -> loadRoomsData());

        JPanel refreshPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        refreshPanel.add(refreshButton);

        panel.add(refreshPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        addButton = new JButton("Add Room");
        updateButton = new JButton("Update Room");
        removeButton = new JButton("Remove Room");
        clearButton = new JButton("Clear Form");

        updateButton.setEnabled(false);
        removeButton.setEnabled(false);

        addButton.addActionListener(e -> addRoom());
        updateButton.addActionListener(e -> updateRoom());
        removeButton.addActionListener(e -> removeRoom());
        clearButton.addActionListener(e -> clearForm());

        panel.add(addButton);
        panel.add(updateButton);
        panel.add(removeButton);
        panel.add(clearButton);

        return panel;
    }

    private void loadBuildings() {
        buildingComboBox.removeAllItems();
        buildingComboBox.addItem("All Buildings"); // Default option
        List<String> buildings = buildingDML.getAllBuildingNames();
        for (String building : buildings) {
            buildingComboBox.addItem(building);
        }
        buildingComboBox.setSelectedIndex(0); // Select "All Buildings" by default
    }

    private void loadFloors() {
        floorComboBox.removeAllItems();
        floorComboBox.addItem("All Floors"); // Default option
        String selectedBuilding = (String) buildingComboBox.getSelectedItem();
        if (selectedBuilding != null && !selectedBuilding.equals("All Buildings") && !selectedBuilding.trim().isEmpty()) {
            int buildingId = buildingDML.getBuildingIdByName(selectedBuilding);
            if (buildingId != -1) {
                List<String> floors = floorDML.getFloorNumbersByBuildingId(buildingId);
                for (String floor : floors) {
                    floorComboBox.addItem(floor);
                }
            }
        }
        floorComboBox.setSelectedIndex(0); // Select "All Floors" by default
    }

    private void loadRoomsData() {
        tableModel.setRowCount(0);
        List<RoomDML.RoomDetails> rooms = roomDML.getAllRoomsWithDetails();
        for (RoomDML.RoomDetails room : rooms) {
            tableModel.addRow(new Object[]{
                    room.roomId,
                    room.buildingName != null ? room.buildingName : "N/A",
                    room.floorNumber != null ? room.floorNumber : "N/A",
                    room.roomNumber,
                    room.electricCounter,
                    room.waterCounter,
                    room.isOccupied ? "Yes" : "No",
                    room.tenantName
            });
        }
    }

    public void refreshAfterBuildingChanges() {
        loadBuildings();
        loadFloors();
        loadRoomsData(); // Load all rooms instead of filtering
    }

    public void refreshAfterFloorChanges() {
        loadFloors();
        loadRoomsData(); // Load all rooms instead of filtering
    }

    private void filterRoomsByBuildingAndFloor() {
        String selectedBuilding = (String) buildingComboBox.getSelectedItem();
        String selectedFloor = (String) floorComboBox.getSelectedItem();

        tableModel.setRowCount(0);
        List<RoomDML.RoomDetails> rooms = roomDML.getAllRoomsWithDetails();

        for (RoomDML.RoomDetails room : rooms) {
            boolean matchesBuilding = selectedBuilding == null || selectedBuilding.equals("All Buildings") ||
                    (room.buildingName != null && room.buildingName.equals(selectedBuilding));
            boolean matchesFloor = selectedFloor == null || selectedFloor.equals("All Floors") ||
                    (room.floorNumber != null && room.floorNumber.equals(selectedFloor));

            if (matchesBuilding && matchesFloor) {
                tableModel.addRow(new Object[]{
                        room.roomId,
                        room.buildingName != null ? room.buildingName : "N/A",
                        room.floorNumber != null ? room.floorNumber : "N/A",
                        room.roomNumber,
                        room.electricCounter,
                        room.waterCounter,
                        room.isOccupied ? "Yes" : "No",
                        room.tenantName
                });
            }
        }
    }

    private void addRoom() {
        String roomNumber = roomNumberField.getText().trim();
        String selectedBuilding = ((String) buildingComboBox.getSelectedItem()).trim();
        String selectedFloor = ((String) floorComboBox.getSelectedItem()).trim();

        if (roomNumber.isEmpty() || selectedBuilding.isEmpty() || selectedFloor.isEmpty() ||
                selectedBuilding.equals("All Buildings") || selectedFloor.equals("All Floors")) {
            JOptionPane.showMessageDialog(this, "Please select a specific building and floor, and enter a room number", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String electricText = electricCounterField.getText().trim();
            String waterText = waterCounterField.getText().trim();

            int electricCounter = electricText.isEmpty() ? 0 : Integer.parseInt(electricText);
            int waterCounter = waterText.isEmpty() ? 0 : Integer.parseInt(waterText);

            // Check for negative numbers
            if (electricCounter < 0 || waterCounter < 0) {
                JOptionPane.showMessageDialog(this, "Counter values cannot be negative", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int buildingId = buildingDML.getBuildingIdByName(selectedBuilding);
            if (buildingId == -1) {
                String address = JOptionPane.showInputDialog(this, "Building not found. Enter building address:");
                if (address == null || address.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Address is required for new building", "Validation Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                Building newBuilding = new Building(selectedBuilding, address);
                buildingDML.saveBuilding(newBuilding);
                buildingId = buildingDML.getBuildingIdByName(selectedBuilding);
                loadBuildings();
                buildingComboBox.setSelectedItem(selectedBuilding);
            }

            int floorId = floorDML.getFloorIdByBuildingAndNumber(buildingId, selectedFloor);
            if (floorId == -1) {
                Floor newFloor = new Floor(selectedFloor);
                floorDML.saveFloor(newFloor, buildingId);
                floorId = floorDML.getFloorIdByBuildingAndNumber(buildingId, selectedFloor);
                loadFloors();
                floorComboBox.setSelectedItem(selectedFloor);
            }

            int existingRoomId = roomDML.getRoomIdByBuildingFloorAndNumber(selectedBuilding, selectedFloor, roomNumber);
            if (existingRoomId != -1) {
                JOptionPane.showMessageDialog(this, "Room with this number already exists in " + selectedBuilding + ", Floor " + selectedFloor, "Duplicate Room", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Room newRoom = new Room(roomNumber, electricCounter, waterCounter);
            roomDML.saveRoom(newRoom, floorId);

            loadRoomsData(); // Reload all rooms
            clearForm();
            JOptionPane.showMessageDialog(this, "Room added successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Counter values must be valid numbers", "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error adding room: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void updateRoom() {
        if (selectedRoomId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room to update", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String roomNumber = roomNumberField.getText().trim();
        String selectedBuilding = (String) buildingComboBox.getSelectedItem();
        String selectedFloor = (String) floorComboBox.getSelectedItem();

        if (roomNumber.isEmpty() || selectedBuilding == null || selectedFloor == null ||
                selectedBuilding.equals("All Buildings") || selectedFloor.equals("All Floors")) {
            JOptionPane.showMessageDialog(this, "Please select a specific building and floor, and enter a room number", "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            String electricText = electricCounterField.getText().trim();
            String waterText = waterCounterField.getText().trim();

            int electricCounter = electricText.isEmpty() ? 0 : Integer.parseInt(electricText);
            int waterCounter = waterText.isEmpty() ? 0 : Integer.parseInt(waterText);

            // Check for negative numbers
            if (electricCounter < 0 || waterCounter < 0) {
                JOptionPane.showMessageDialog(this, "Counter values cannot be negative", "Validation Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            Room existingRoom = roomDML.getRoomById(selectedRoomId);
            if (existingRoom == null) {
                JOptionPane.showMessageDialog(this, "Room not found", "Database Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int duplicateRoomId = roomDML.getRoomIdByBuildingFloorAndNumber(selectedBuilding, selectedFloor, roomNumber);
            if (duplicateRoomId != -1 && duplicateRoomId != selectedRoomId) {
                JOptionPane.showMessageDialog(this, "Another room with this number already exists in " + selectedBuilding + ", Floor " + selectedFloor, "Duplicate Room", JOptionPane.ERROR_MESSAGE);
                return;
            }

            existingRoom.setRoomNumber(roomNumber);
            roomDML.updateRoom(selectedRoomId, existingRoom);
            roomDML.updateRoomCounters(selectedRoomId, electricCounter, waterCounter);

            loadRoomsData(); // Reload all rooms
            clearForm();
            JOptionPane.showMessageDialog(this, "Room updated successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Counter values must be valid numbers", "Validation Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating room: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeRoom() {
        if (selectedRoomId == -1) {
            JOptionPane.showMessageDialog(this, "Please select a room to remove", "Selection Required", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete this room?",
                "Confirm Deletion",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);

        if (confirm == JOptionPane.YES_OPTION) {
            Room room = roomDML.getRoomById(selectedRoomId);
            if (room != null && room.isOccupied()) {
                JOptionPane.showMessageDialog(this, "Cannot delete an occupied room", "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            roomDML.deleteRoom(selectedRoomId);
            loadRoomsData(); // Reload all rooms
            clearForm();
            JOptionPane.showMessageDialog(this, "Room removed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void clearForm() {
        roomNumberField.setText("");
        electricCounterField.setText("");
        waterCounterField.setText("");
        selectedRoomId = -1;
        roomTable.clearSelection();
        updateButton.setEnabled(false);
        removeButton.setEnabled(false);
    }

    public static void showRoomManagement(Landlord landlord) {
        JFrame frame = new JFrame("Room Management");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setSize(900, 700);
        frame.add(new RoomManagementGUI(landlord));
        frame.setVisible(true);
    }
}