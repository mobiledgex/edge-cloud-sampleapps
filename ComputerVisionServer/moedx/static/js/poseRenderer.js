/**
 * The bonePairs array is a 2D list of the body parts that should be connected together.
 * E.g., 1 for "Neck", 2 for "RShoulder", etc.
 * See https://github.com/CMU-Perceptual-Computing-Lab/openpose/blob/master/doc/output.md
 */
const bonePairs = [
  [1,8],[1,2],[1,5],[2,3],[3,4],[5,6],[6,7],[8,9],[9,10],[10,11],[8,12],
  [12,13],[13,14],[1,0],[0,15],[15,17],[0,16],[16,18],[14,19],
  [19,20],[14,21],[11,22],[22,23],[11,24]
];

/**
 * Colors array corresponding to the "bonePairs" array above. For example, the first pair of
 * coordinates [1,8] will be drawn with the first color in this array, "#ff0055".
 */
const boneColors = [
  "#ff0055", "#ff0000", "#ff5500", "#ffaa00", "#ffff00", "#aaff00", "#55ff00", "#00ff00",
  "#ff0000", "#00ff55", "#00ffaa", "#00ffff", "#00aaff", "#0055ff", "#0000ff", "#ff00aa",
  "#aa00ff", "#ff00ff", "#5500ff", "#0000ff", "#0000ff", "#0000ff", "#00ffff", "#00ffff",
  "#00ffff"
];

var jointCircleRadius = 6;
var lineWidth = 4;

function renderPoses(ctx, poses, renderScale, animationAlpha) {
  console.log("renderPoses");
  console.log(poses);
  let totalPoses = poses.length;
  for (let i = 0; i < totalPoses; i++) {
    let pose = poses[i];
    for(let j = 0; j < bonePairs.length; j++) {
      let pair = bonePairs[j];
      let indexStart = pair[0];
      let indexEnd = pair[1];
      // console.log("indexStart="+indexStart+" indexEnd="+indexEnd);

      let keypoint1 = pose[indexStart];
      let x1 = keypoint1[0] * renderScale;
      let y1 = keypoint1[1] * renderScale;
      let score1 = keypoint1[2];

      let keypoint2 = pose[indexEnd];
      let x2 = keypoint2[0] * renderScale;
      let y2 = keypoint2[1] * renderScale;
      let score2 = keypoint2[2];

      if(score1 == 0 || score2 == 0) {
          continue;
      }

      // console.log("Drawing indexStart="+indexStart+" indexEnd="+indexEnd+" ("+x1+","+y1+","+x2+","+y2+")");

      ctx.strokeStyle = convertHexToRGBA(boneColors[j], animationAlpha);
      ctx.lineWidth = lineWidth;
      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.stroke();

      ctx.fillStyle = convertHexToRGBA(boneColors[j], animationAlpha);
      ctx.moveTo(x1, y1);
      ctx.arc(x1, y1, jointCircleRadius, 0, 2*Math.PI);
      ctx.moveTo(x2, y2);
      ctx.arc(x2, y2, jointCircleRadius, 0, 2*Math.PI);
      ctx.stroke();
      ctx.fill();
    }
  }
}
