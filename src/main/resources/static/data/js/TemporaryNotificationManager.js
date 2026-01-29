// TemporaryNotificationManager.js - Complete Implementation with Error Handling
class TemporaryNotificationManager {
    constructor(employeeId) {
        console.log("Initializing TemporaryNotificationManager for:", employeeId);
        this.employeeId = employeeId;
        this.tempNotifications = new Map();
        this.initialize();
    }

    initialize() {
        try {
            this.loadFromStorage();
            this.cleanupOld();
            console.log("TemporaryNotificationManager initialized successfully");
        } catch (error) {
            console.error("Failed to initialize TemporaryNotificationManager:", error);
            this.tempNotifications = new Map();
        }
    }

    addTemporaryNotification(notification) {
        try {
            const id = 'temp_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
            const tempNotification = {
                id: id,
                type: notification.type || 'INFO',
                title: notification.title || 'Notification',
                message: notification.message || '',
                createdAt: notification.createdAt || new Date().toISOString(),
                read: false,
                persistent: false,
                color: this.getColorForType(notification.type),
                icon: this.getIconForType(notification.type),
                timestamp: this.format12HourTime(notification.createdAt || new Date().toISOString())
            };

            this.tempNotifications.set(id, tempNotification);
            this.saveToStorage();
            console.log("Added temporary notification:", tempNotification.id);
            return tempNotification;
        } catch (error) {
            console.error("Error adding temporary notification:", error);
            return notification;
        }
    }

    getUnreadCount() {
        try {
            return Array.from(this.tempNotifications.values())
                .filter(n => !n.read).length;
        } catch (error) {
            console.error("Error getting unread count:", error);
            return 0;
        }
    }

    getTemporaryNotifications() {
        try {
            return Array.from(this.tempNotifications.values())
                .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        } catch (error) {
            console.error("Error getting temporary notifications:", error);
            return [];
        }
    }

    markAsRead(id) {
        try {
            const notification = this.tempNotifications.get(id);
            if (notification) {
                notification.read = true;
                this.saveToStorage();
                console.log("Marked notification as read:", id);
            }
        } catch (error) {
            console.error("Error marking as read:", error);
        }
    }

    markAllAsRead() {
        try {
            this.tempNotifications.forEach(n => n.read = true);
            this.saveToStorage();
            console.log("Marked all temporary notifications as read");
        } catch (error) {
            console.error("Error marking all as read:", error);
        }
    }

    removeReadTemporary() {
        try {
            let deletedCount = 0;
            for (const [id, notification] of this.tempNotifications) {
                if (notification.read) {
                    this.tempNotifications.delete(id);
                    deletedCount++;
                }
            }
            if (deletedCount > 0) {
                this.saveToStorage();
                console.log(`Removed ${deletedCount} read temporary notifications`);
            }
        } catch (error) {
            console.error("Error removing read notifications:", error);
        }
    }

    deleteNotification(id) {
        try {
            if (this.tempNotifications.delete(id)) {
                this.saveToStorage();
                console.log("Deleted notification:", id);
                return true;
            }
            return false;
        } catch (error) {
            console.error("Error deleting notification:", error);
            return false;
        }
    }

    cleanupOld() {
        try {
            const oneDayAgo = Date.now() - (24 * 60 * 60 * 1000);
            let deletedCount = 0;

            for (const [id, notification] of this.tempNotifications) {
                const created = new Date(notification.createdAt).getTime();
                if (created < oneDayAgo) {
                    this.tempNotifications.delete(id);
                    deletedCount++;
                }
            }

            if (deletedCount > 0) {
                this.saveToStorage();
                console.log(`🗑️ Cleaned up ${deletedCount} old temporary notifications`);
            }
            return deletedCount;
        } catch (error) {
            console.error("Error cleaning up old notifications:", error);
            return 0;
        }
    }

    loadFromStorage() {
        try {
            const saved = localStorage.getItem(`tempNotifications_${this.employeeId}`);
            if (saved) {
                const data = JSON.parse(saved);
                if (Array.isArray(data)) {
                    this.tempNotifications = new Map(data);
                } else {
                    console.warn("Invalid data format in localStorage");
                    this.tempNotifications = new Map();
                }
            } else {
                this.tempNotifications = new Map();
            }
        } catch (e) {
            console.error('Failed to load temp notifications from storage:', e);
            this.tempNotifications = new Map();
        }
    }

    saveToStorage() {
        try {
            const data = JSON.stringify(Array.from(this.tempNotifications.entries()));
            localStorage.setItem(`tempNotifications_${this.employeeId}`, data);
        } catch (e) {
            console.error('Failed to save temp notifications to storage:', e);
        }
    }

    getColorForType(type) {
        if (!type) return 'primary';
        if (type.includes('VERIFIED')) return 'success';
        if (type.includes('REJECTED')) return 'danger';
        if (type.includes('WARNING')) return 'warning';
        if (type.includes('DEADLINE')) return 'warning';
        if (type.includes('UPLOADED')) return 'info';
        return 'primary';
    }

    getIconForType(type) {
        if (!type) return 'fa-bell';
        if (type.includes('VERIFIED')) return 'fa-check-circle';
        if (type.includes('REJECTED')) return 'fa-times-circle';
        if (type.includes('DEADLINE_WARNING')) return 'fa-clock';
        if (type.includes('DEADLINE_REACHED')) return 'fa-exclamation-triangle';
        if (type.includes('UPLOADED')) return 'fa-file-upload';
        return 'fa-bell';
    }

    format12HourTime(dateString) {
        try {
            const date = new Date(dateString);
            if (isNaN(date.getTime())) return 'Invalid date';

            return date.toLocaleString('en-US', {
                day: '2-digit',
                month: 'short',
                year: 'numeric',
                hour: '2-digit',
                minute: '2-digit',
                hour12: true
            });
        } catch (error) {
            console.error("Error formatting time:", error);
            return 'Unknown time';
        }
    }
}

// Make it globally available
if (typeof window !== 'undefined') {
    window.TemporaryNotificationManager = TemporaryNotificationManager;
}