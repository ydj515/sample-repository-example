import http from "k6/http";
import { check, sleep } from "k6";

export const options = {
  vus: 100,
  duration: "1m"
  //   stages: [
  //     { duration: '5m', target: 100 }, // traffic ramp-up from 1 to 100 users over 5 minutes.
  //     { duration: '30m', target: 100 }, // stay at 100 users for 30 minutes
  //     { duration: '5m', target: 0 }, // ramp-down to 0 users
  // ]
};

export default function () {
  http.get("http://localhost:8080");
  // sleep(1); // 각 요청 사이 1초 대기 넣을까?
}
